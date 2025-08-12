#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#include "memory.h"
#include "fail.h"
#include "engine.h"

#if GC_VERSION == GC_MARK_N_SWEEP

static void* memory_start = NULL;
static void* memory_end = NULL;

static uvalue_t* bitmap_start = NULL;

static value_t* heap_start = NULL;
static value_t* heap_end = NULL;
static value_t heap_start_v = 0;
static value_t heap_end_v = 0;
static value_t* heap_first_block = NULL;

#define FREE_LISTS_COUNT 32
static value_t* free_list_heads[FREE_LISTS_COUNT];

#define MIN_BLOCK_SIZE 1
#define HEADER_SIZE 1

// Header management

static value_t header_pack(tag_t tag, value_t size) {
  return (size << 8) | (value_t)tag;
}

static tag_t header_unpack_tag(value_t header) {
  return (tag_t)(header & 0xFF);
}

static value_t header_unpack_size(value_t header) {
  return header >> 8;
}

// Bitmap management

static int bitmap_is_bit_set(value_t* ptr) {
  assert(heap_start <= ptr && ptr < heap_end);
  long index = ptr - heap_start;
  long word_index = index / (long)VALUE_BITS;
  long bit_index = index % (long)VALUE_BITS;
  return (bitmap_start[word_index] & ((uvalue_t)1 << bit_index)) != 0;
}

static void bitmap_set_bit(value_t* ptr) {
  assert(heap_start <= ptr && ptr < heap_end);
  long index = ptr - heap_start;
  long word_index = index / (long)VALUE_BITS;
  long bit_index = index % (long)VALUE_BITS;
  bitmap_start[word_index] |= (uvalue_t)1 << bit_index;
}

static void bitmap_clear_bit(value_t* ptr) {
  assert(heap_start <= ptr && ptr < heap_end);
  long index = ptr - heap_start;
  long word_index = index / (long)VALUE_BITS;
  long bit_index = index % (long)VALUE_BITS;
  bitmap_start[word_index] &= ~((uvalue_t)1 << bit_index);
}

// Virtual <-> physical address translation

static void* addr_v_to_p(value_t v_addr) {
  return (char*)memory_start + v_addr;
}

static value_t addr_p_to_v(void* p_addr) {
  return (value_t)((char*)p_addr - (char*)memory_start);
}

// Free lists management

static value_t real_size(value_t size) {
  assert(0 <= size);
  return size < MIN_BLOCK_SIZE ? MIN_BLOCK_SIZE : size;
}

static unsigned int free_list_index(value_t size) {
  assert(0 <= size);
  return size >= FREE_LISTS_COUNT ? FREE_LISTS_COUNT - 1 : (unsigned int)size;
}

char* memory_get_identity() {
  return "mark & sweep garbage collector";
}

void memory_setup(size_t total_byte_size) {
  memory_start = malloc(total_byte_size);
  if (memory_start == NULL)
    fail("cannot allocate %zd bytes of memory", total_byte_size);
  memory_end = (char*)memory_start + total_byte_size;
}

void memory_cleanup() {
  assert(memory_start != NULL);
  free(memory_start);

  memory_start = memory_end = NULL;
  bitmap_start = NULL;
  heap_start = heap_end = NULL;
  heap_start_v = heap_end_v = 0;
  for (int l = 0; l < FREE_LISTS_COUNT; ++l)
    free_list_heads[l] = NULL;
}

void* memory_get_start() {
  return memory_start;
}

void* memory_get_end() {
  return memory_end;
}

void memory_set_heap_start(void* ptr) {
  assert(memory_start <= ptr && ptr < memory_end);

  const size_t bh_size =
    (size_t)((char*)memory_end - (char*)ptr) / sizeof(value_t);

  const size_t bitmap_size = (bh_size - 1) / (VALUE_BITS + 1) + 1;
  const size_t heap_size = bh_size - bitmap_size;

  bitmap_start = ptr;
  memset(bitmap_start, 0, bitmap_size * sizeof(value_t));

  heap_start = (value_t*)bitmap_start + bitmap_size;
  heap_end = heap_start + heap_size;
  assert(heap_end == memory_end);

  heap_start_v = addr_p_to_v(heap_start);
  heap_end_v = addr_p_to_v(heap_end);

  heap_first_block = heap_start + HEADER_SIZE;
  const value_t initial_block_size = (value_t)(heap_end - heap_first_block);
  heap_first_block[-1] = header_pack(tag_None, initial_block_size);
  heap_first_block[0] = 0;

  for (int l = 0; l < FREE_LISTS_COUNT - 1; ++l)
    free_list_heads[l] = memory_start;
  free_list_heads[FREE_LISTS_COUNT - 1] = heap_first_block;
}

/* ------------------------------------------------------------------------- */
/*                          HELPERS FUNCTIONS                                */
/* ------------------------------------------------------------------------- */

/* Return the first block large enough to accommodate “need”, or NULL          */
/* “prev” receives the block that precedes “curr” in the big‑block free list.  */
static value_t *find_fit_in_last_list(value_t need, 
                                      value_t **prev /* may be NULL */) {
  value_t *p = free_list_heads[FREE_LISTS_COUNT - 1];
  value_t *q = NULL;                     /* q  = previous element            */
  while (p != memory_start) {            /* memory_start is the sentinel     */
    value_t sz = header_unpack_size(p[-1]);
    if (sz >= need) {
      if (prev) *prev = q;
      return p;                          /* first‑fit strategy               */
    }
    q = p;
    p = addr_v_to_p(p[0]);               /* follow the virtual “next” field  */
  }
  return NULL;                           /* nothing large enough             */
}

/* Push a FREE block on the adequate size class list (at the head)            */
static inline void push_on_list(value_t *blk, value_t sz) {
  unsigned idx            = free_list_index(sz);
  value_t  *old_head      = free_list_heads[idx];
  blk[0]                  = addr_p_to_v(old_head); /* store next as V‑addr   */
  free_list_heads[idx]    = blk;
}

/* Pop and return the first element of list “idx”.  Caller guarantees         */
/* the list is non‑empty.                                                     */
static inline value_t *pop_from_list(unsigned idx) {
  value_t *head   = free_list_heads[idx];
  value_t *next   = addr_v_to_p(head[0]);
  free_list_heads[idx] = next;
  return head;
}

/* A block has logical size 0  ⇔
      – its physical capacity is exactly MIN_BLOCK_SIZE, and
      – word 0 was left 0 by allocate()                              */
static inline int block_is_logical_size_zero(const value_t *blk) {
  return header_unpack_size(blk[-1]) == MIN_BLOCK_SIZE  && blk[0] == 0;
}

/* ------------------------------------------------------------------------- */
/*                      MAIN ALLOCATOR (no GC attempt)                       */
/* ------------------------------------------------------------------------- */
static value_t* allocate(tag_t tag, value_t size) {
  /* ----------- 1. sanitise request ------------------------------------- */
  assert(size >= 0);
  value_t need = real_size(size);              /* force minimal payload     */
  const unsigned wanted_idx = free_list_index(need);

  /* ----------- 2. try an exact‑size or size‑class hit ------------------ */
  if (wanted_idx < FREE_LISTS_COUNT - 1) {     /* “small” segregated lists  */
    if (free_list_heads[wanted_idx] != memory_start) {
      value_t *blk = pop_from_list(wanted_idx);
      blk[-1]      = header_pack(tag, size);   /* store logical size  */
      bitmap_set_bit(blk);

      /* If the user really asked for a size-0 block, remember it
        Word 0 is innaccessible to the user becase BSIZ==0 forbids BGET/BSET */
      if (size == 0)
        blk[0] = 0;
      return blk;
    }
  }

  /* ----------- 3. fall back on the variable‑size list ------------------ */
  value_t *prev = NULL;
  value_t *fit  = find_fit_in_last_list(need, &prev);
  if (fit == NULL)                     /* no room – caller will trigger GC */
    return NULL;

  /* remove “fit” from the big‑block list */
  value_t *next = addr_v_to_p(fit[0]);
  if (prev == NULL)
    free_list_heads[FREE_LISTS_COUNT - 1] = next;
  else
    prev[0] = addr_p_to_v(next);

  /* block size in words (payload only, WITHOUT header) */
  value_t fit_sz = header_unpack_size(fit[-1]);

  /* ----------- 3a. split if it would leave a useable tail -------------- */
  const value_t leftover_cap = fit_sz - need;
  if (leftover_cap >= MIN_BLOCK_SIZE + HEADER_SIZE) {
    /* carve the first “need” words for the user */
    value_t *user_blk = fit;
    user_blk[-1]      = header_pack(tag, size); /* logical size */
    bitmap_set_bit(user_blk);

    if (size == 0) {
      user_blk[0] = 0;
    }

    /* create the tail block and put it back into free lists */
    value_t *tail     = user_blk + need + HEADER_SIZE;
    const value_t tail_sz = fit_sz - need - HEADER_SIZE;
    tail[-1]          = header_pack(tag_None, tail_sz);
    push_on_list(tail, tail_sz);               /* tail is still free        */
    bitmap_set_bit(tail);                      /* mark it as free          */

    return user_blk;
  }

  /* ----------- 3b. otherwise give the whole block to the user ---------- */
  fit[-1] = header_pack(tag, size); /* logical size */
  bitmap_set_bit(fit);
  if (size == 0) {
    fit[0] = 0;                     /* remember logical size 0 */
  }
  return fit;
}

/* ------------------------------------------------------------------ */
/*  Depth‑first mark :                                                */
/*    ‑ ignore pointers outside the heap                              */
/*    ‑ stop if the block is already marked live (bitmap bit cleared) */
/*    ‑ clear the bit (=> live) and recursively visit every word that */
/*      looks like a heap pointer AND still has its bit set           */
/* ------------------------------------------------------------------ */
static void mark(value_t* blk)
{
  /* 1 . validate pointer is really inside the heap area */
  if (blk < heap_start || blk >= heap_end) return;

  /* 2 . if bitmap bit already 0 → block was visited => stop */
  if (!bitmap_is_bit_set(blk)) return;

  /* 3 . mark it live by clearing the bit */
  bitmap_clear_bit(blk);

  /* 4 . walk over the payload words */
  const value_t words = header_unpack_size(blk[-1]);
  for (value_t i = 0; i < words; ++i) {

    value_t val = blk[i];

    /* --- fast tests to reject non‑pointers ------------------------ */
    if ((val & 0x3) != 0)                  /* low‑two bits ≠ 00 → not a ref */
      continue;
    if (val < heap_start_v || val >= heap_end_v)   /* outside heap range   */
      continue;

    /* --- convert virtual → physical address ---------------------- */
    value_t* child = addr_v_to_p(val);

    /* only recurse if the child still looks unvisited (bit = 1)     */
    if (bitmap_is_bit_set(child))
      mark(child);
  }
}

/* ------------------------------------------------------------------ */
/*  Sweep phase                                                       */
/*  – free‑lists are emptied first                                    */
/*  – walk the heap linearly                                          */
/*      * bit = 1  ⟹  unreachable  →  collect                        */
/*      * bit = 0  ⟹  live         →  re‑arm bit to 1 for next round */
/*  – consecutive unreachable blocks are coalesced into one           */
/*  – every fresh free block is pushed on the appropriate list        */
/* ------------------------------------------------------------------ */
static void sweep(void)
{
  /* 0 . discard old free lists (they will be rebuilt) */
  for (unsigned i = 0; i < FREE_LISTS_COUNT; ++i)
    free_list_heads[i] = memory_start;         /* sentinel = empty list */

  /* 1 . linear scan variables */
  value_t* ptr         = heap_first_block;      /* current block        */
  value_t* span_start  = NULL;                  /* start of free run    */
  value_t  span_size   = 0;                     /* payload words in run */

  while (ptr < heap_end) {

    /* fetch payload size and compute address of next block */
    value_t words = header_unpack_size(ptr[-1]);
    value_t* next = ptr + words + HEADER_SIZE;

    if (bitmap_is_bit_set(ptr)) {               /* ❶ UNREACHABLE block  */
      /* accumulate into current free span */
      if (span_start == NULL) {
        span_start = ptr;
        span_size  = words;
      } else {
        span_size += HEADER_SIZE + words;       /* merge with previous  */
      }
    } else {                                    /* ❷ LIVE block         */
      /* reset bit to 1 so next GC starts clean */
      bitmap_set_bit(ptr);

      /* if we were coalescing, finalise that free span now            */
      if (span_start != NULL) {
        span_start[-1] = header_pack(tag_None, span_size);
        bitmap_set_bit(span_start);             /* mark it as free       */
        push_on_list(span_start, span_size);    /* add to seg. list     */
        span_start = NULL;                      /* reset coalescer      */
      }
    }

    ptr = next;
  }

  /* 3 . flush a pending free span that reaches heap_end */
  if (span_start != NULL) {
    span_start[-1] = header_pack(tag_None, span_size);
    bitmap_set_bit(span_start);
    push_on_list(span_start, span_size);
  }
}

value_t* memory_allocate(tag_t tag, value_t size) {
  value_t* first_try = allocate(tag, size);
  if (first_try != NULL)
    return first_try;

  value_t* lb0 = engine_get_Lb();
  for (int i = 0; i < 6; ++i)
    if (lb0 + i*32 != (value_t*)memory_start)          /* skip sentinel */
      mark(lb0 + i*32);

  value_t* lb = engine_get_Lb();
  if (lb != memory_start) mark(lb);
  value_t* ib = engine_get_Ib();
  if (ib != memory_start) mark(ib);
  value_t* ob = engine_get_Ob();
  if (ob != memory_start) mark(ob);

  sweep();

  value_t* second_try = allocate(tag, size);
  if (second_try != NULL)
    return second_try;

  fail("\ncannot allocate %d words of memory, even after GC\n", size);
}

value_t memory_get_block_size(value_t* block) {
  // return header_unpack_size(block[-1]);
  /* logical == 0 is a special case we encoded with one extra word     */
  value_t logical = header_unpack_size(block[-1]);
  return (logical == 0 && block_is_logical_size_zero(block)) ? 0
                                                          : logical;
}

tag_t memory_get_block_tag(value_t* block) {
  return header_unpack_tag(block[-1]);
}

#endif