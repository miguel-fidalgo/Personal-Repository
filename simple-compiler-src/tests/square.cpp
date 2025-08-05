#include <stdio.h>

class Square {
public:
   long side;

   Square(long side) {
      this->side = side;
   }
  
   long area() {
     return this->side * this->side;
   }
};

int
main()
{
   Square* square;
   square = new Square(5);
   printf("Area=%ld\n", square->area() );
}

