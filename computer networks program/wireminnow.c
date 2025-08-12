#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <arpa/inet.h> // For using ntohs()
#include <netdb.h>     // For using gethostbyaddr()
#include <string.h>    // For using strcmp()
#include <ctype.h>     // For isprint()

#define K 80

// Struct definition for PCAP global header
struct pcaphdr {
    uint32_t magic_number; // unsigned integer of 32 bits
    uint16_t version_major; // unsigned integer of 16 bits
    uint16_t version_minor; // unsigned integer of 16 bits
    uint32_t reserved_1; // reserved 1
    uint32_t reserved_2; // reserved 2
    uint32_t snaplen; // unsigned integer of 32 bits
    uint32_t network; // unsigned integer of 32 bits
};

// Struct definition for header PCAP packet header
struct pcappkt {
    uint32_t ts_sec; // Timestamp(seconds)
    uint32_t ts_msec; // Timestamp(microseconds/nanoseconds)
    uint32_t c_len; // Captured Packet Length
    uint32_t len; // Original Packet Length
};

// Struct definition for Enthernet header
struct ethhdr {
    uint8_t dst[6];  // Destination MAC address
    uint8_t src[6];  // Source MAC address
    uint16_t type;   // Type field (e.g., 0x0800 for IP)
};

// Struct definition for IPv4 header
struct ipv4hdr {
    uint8_t version_ihl;    // Version and Internet Header Length (IHL)
    uint8_t tos;            // Type of Service
    uint16_t tot_len;       // Total Length
    uint16_t id;            // Identification
    uint16_t frag_off;      // Flags + Fragment Offset
    uint8_t ttl;            // Time to Live
    uint8_t protocol;       // Protocol (e.g., TCP, UDP)
    uint16_t checksum;      // Header Checksum
    uint32_t src_addr;      // Source Address
    uint32_t dst_addr;      // Destination Address
};

// Struct definition for ARP header
struct arphdr {
    uint16_t hw_type;           // Hardware type
    uint16_t proto_type;        // Protocol type
    uint8_t hw_addr_len;        // Hardware address length
    uint8_t proto_addr_len;     // Protocol address length
    uint16_t op;                // Operation (request or reply)
    uint8_t sender_hw_addr[6];  // Sender hardware address
    uint32_t sender_ip_addr;    // Sender IP address
    uint8_t target_hw_addr[6];  // Target hardware address
    uint32_t target_ip_addr;    // Target IP address
};

// Struct definition for ICMP header
struct icmphdr {
    uint8_t type;          // Type field
    uint8_t code;          // Code field
    uint16_t checksum;     // Checksum
    uint16_t identifier;   // Identifier
    uint16_t sequence;     // Sequence Number
};

// Struct definition for UDP header
struct udphdr {
    uint16_t src_port;      // Source port
    uint16_t dest_port;     // Destination port
    uint16_t length;        // Message length
    uint16_t checksum;      // Checksum
};

// Struct definition for TCP header
struct tcphdr {
    uint16_t src_port;      // Source port
    uint16_t dest_port;     // Destination port
    uint32_t seq_num;       // Sequence number
    uint32_t ack_num;       // Acknowledgment number
    uint8_t data_offset_reserved_ns; // Data offset, reserved bits, NS flag
    uint8_t flags;          // Control flags
    uint16_t window;        // Window size
    uint16_t checksum;      // Checksum
    uint16_t urgent_pointer;// Urgent pointer
};

// Function to print MAC address with the given format (6 bytes per MAC address)
void print_mac_address(uint8_t *mac) {
    printf("%02x:%02x:%02x:%02x:%02x:%02x", 
        mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
}

// Function to print IP address or domain name based on the use_ip_names flag
void print_ip_address(uint32_t ip, int use_ip_names) {
    struct in_addr addr;
    addr.s_addr = ip; // IP is in network byte order

    if (use_ip_names) {
        struct hostent *he = gethostbyaddr(&addr, sizeof(addr), AF_INET);
        if (he != NULL) {
            printf("%s", he->h_name);
        } else {
            // If reverse lookup fails, print dotted decimal
            printf("%d.%d.%d.%d", (ip & 0xff), (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
            //printf("%s", inet_ntoa(addr));
        }
    } else {
        printf("%d.%d.%d.%d", (ip & 0xff), (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
        //printf("%s", inet_ntoa(addr));
    }
}

void print_protocol(uint8_t protocol) {
    char *protocol_name;
    switch(protocol) {
        case 1:
            protocol_name = "ICMP";
            break;
        case 2:
            protocol_name = "IGMP";
            break;
        case 3:
            protocol_name = "GCP";
            break;
        case 4:
            protocol_name = "IPv4";
            break;
        case 6:
            protocol_name = "TCP";
            break;
        case 17:
            protocol_name = "UDP";
            break;
        case 103:
            protocol_name = "PIM";
            break;
        default:
            protocol_name = NULL;
            break;
    }
    if (protocol_name != NULL) {
        printf(" typ=%s\n", protocol_name);
    } else {
        printf(" typ=%u\n", protocol);
    }
}

// Read and parses the header from the captured file
int parse_pcap_packets(FILE *file, int name_flag, int arp_flag, int imcmp_verbose) {
    struct pcappkt pkt_header;
    int packet_count = 0;

    // fseek to jump the pcap global header
    if (fseek(file, sizeof(struct pcaphdr), SEEK_SET) != 0) {
        perror("Error when iterating to next package.\n");
        exit(1);
    }

    // Iterate through all packets in the file
    while(fread(&pkt_header, sizeof(struct pcappkt), 1, file) == 1) {
        packet_count++;

        // Print packet length
        printf("Packet with captured length %u\n", pkt_header.c_len);

        // Check if the package size is correct and return an error message if not
        if (pkt_header.c_len > pkt_header.len) {
            fprintf(stderr, "Error: captured length (%u) is bigger than original length (%u) in the package %d.\n", 
            pkt_header.c_len, pkt_header.len, packet_count);
        }

        long bytes_read = 0;

        // Read Ethernet header
        struct ethhdr eth_header;
        if (fread(&eth_header, sizeof(struct ethhdr), 1, file) != 1) {
            perror("Error reading Ethernet header.\n");
            exit(1);
        }

        bytes_read += sizeof(struct ethhdr);        

        // Print Ethernet header
        printf("Ether: dst=");
        print_mac_address(eth_header.dst);
        printf(" src=");
        print_mac_address(eth_header.src);

        // Convert the type field from network byte (big-endian) to 
        // host byte order (can either be big-endian or little-endian)
        uint16_t eth_type = ntohs(eth_header.type);
        printf(" typ=%04x \n", eth_type);

        // Check if the Ethernet frame carries IPv4 (type == 0x0800)
        if (eth_type == 0x0800){
            // Read the initial 20 bytes of the IPv4 header
            uint8_t ip_header_buf[60]; // Maximum size of IP header with options
            if (fread(ip_header_buf, 20, 1, file) != 1){
                perror("Error reading IPv4 header.\n");
                exit(1);
            }
            bytes_read += 20;

            struct ipv4hdr *ip_header = (struct ipv4hdr *)ip_header_buf;

            // Parse the version and IHL
            uint8_t ihl = ip_header->version_ihl & 0x0F; // Lower 4 bits
            // uint8_t version = ip_header->version_ihl >> 4; // Upper 4 bits

            // Calculate IP header length
            uint16_t ip_header_len = ihl * 4;

            // Read IP options if present
            if (ip_header_len > 20) {
                uint16_t options_len = ip_header_len - 20;
                if (fread(&ip_header_buf[20], options_len, 1, file) != 1) {
                    perror("Error reading IPv4 options.\n");
                    exit(1);
                }
                bytes_read += options_len;
            }
            
            // Extract the protocol -> IP "type"
            uint8_t protocol = ip_header->protocol;

            // Print IPv4 header details
            printf("IP: src=");
            print_ip_address(ip_header->src_addr, name_flag);
            printf(" dst=");
            print_ip_address(ip_header->dst_addr, name_flag);
            print_protocol(protocol);

            if (protocol == 1) {
                // ICMP packet
                struct icmphdr icmp_header;
                if (fread(&icmp_header, sizeof(struct icmphdr), 1, file) != 1){
                    perror("Error reading ICMP header.\n");
                    exit(1);
                }
                bytes_read += sizeof(struct icmphdr);

                // Process ICMP header fields
                uint8_t type = icmp_header.type;
                uint8_t code = icmp_header.code;
                uint16_t identifier = ntohs(icmp_header.identifier);
                uint16_t sequence = ntohs(icmp_header.sequence);

                // Map Type to Request/Reply
                const char *icmp_type_str;
                if (type == 8 && code == 0) {
                    icmp_type_str = "Request";
                } else if (type == 0 && code == 0) {
                    icmp_type_str = "Reply";
                } else {
                    icmp_type_str = "Unknown";
                }

                // Print ICMP line
                printf("ICMP: %s, ident=%u seq=%u\n", icmp_type_str, identifier, sequence);

                // Convert total length from network to host byte order
                uint16_t total_length = ntohs(ip_header->tot_len);

                // Calculate payload length
                int payload_length = total_length - ip_header_len - sizeof(struct icmphdr);

                // Ensure we do not read beyond the captured length
                int remaining_bytes_in_packet = pkt_header.c_len - bytes_read;
                if (payload_length > remaining_bytes_in_packet) {
                    payload_length = remaining_bytes_in_packet;
                }

                // Read and print payload
                uint8_t payload[32];
                int payload_to_read = (payload_length >= 32) ? 32 : payload_length;

                if (payload_to_read > 0) {
                    if (fread(payload, payload_to_read, 1, file) != 1) {
                        perror("Error reading ICMP payload.\n");
                        exit(1);
                    }
                    bytes_read += payload_to_read;

                    if (imcmp_verbose) {
                        // Handle the -icmpver option
                        int initial_payload_length = (payload_to_read >=16) ? 16 : payload_to_read;
                        printf("Initial payload: ");

                        // For each byte in the initial payload, check if it's printable
                        for (int i = 0; i < initial_payload_length; i++) {
                            if (isprint(payload[i])) {
                                printf("%c", payload[i]);
                            } else {
                                printf(".");
                            }
                        }
                        printf("\n");

                        // Then, display the remaining payload in hex
                        int remaining_payload_length = payload_to_read - initial_payload_length;
                        if (remaining_payload_length > 0) {
                            printf("Remaining payload: ");
                            for (int i = initial_payload_length; i < payload_to_read; i++) {
                                printf("%02x ", payload[i]);
                            }
                            printf("\n");
                        }
                    } else {
                        // Print the payload in hex, 16 bytes per line
                        printf("Start of payload: ");
                        for (int i = 0; i < payload_to_read; i++) {
                            printf("%02x ", payload[i]);
                            if ((i + 1) % 16 == 0 && (i + 1) != payload_to_read) {
                                printf("\n");
                            }
                        }
                        printf("\n");
                    }
                }
            } else if (protocol == 17) {
                // UDP Packet
                struct udphdr udp_header;
                if (fread(&udp_header, sizeof(struct udphdr), 1, file) != 1) {
                    perror("Error reading UDP Packet.\n");
                    exit(1);
                }
                
                bytes_read += sizeof(struct udphdr);
                // Now we use the ntohs() for converting from network byte order
                // (big-endian) to host byte order (can be either big/little-endian)
                uint16_t src_port = ntohs(udp_header.src_port);
                uint16_t dst_port = ntohs(udp_header.dest_port);
                uint16_t length = ntohs(udp_header.length);

                printf("UDP: srcport=%u dstport=%u len=%u\n", src_port, dst_port, length);

                // Read and print the first 32 bytes of the payload
                int payload_length = length - sizeof(struct udphdr);
                uint8_t payload[32];
                int payload_to_read = (payload_length >= 32) ? 32 : payload_length;

                if (payload_to_read > 0) {
                    if (fread(payload, payload_to_read, 1, file) != 1) {
                        perror("Error reading UDP Payload.\n");
                        exit(1);
                    }
                    bytes_read += payload_to_read;

                    printf("start of payload:");
                    for (int i = 0; i < payload_to_read; i++) {
                        if (isprint(payload[i])) {
                            // If it is a printable character, print a blank and character
                            printf(" %c", payload[i]);
                        } else {
                            // If not print the byte in hex
                            printf(" %02x", payload[i]);
                        }
                    }
                    printf("\n");
                }
            } else if (protocol == 6) {
                // TCP Packet
                // Read the minimum TCP header size (20 Bytes)
                struct tcphdr tcp_header;
                if (fread(&tcp_header, sizeof(struct tcphdr), 1, file) != 1) {
                    perror("Error reading the TCP Packet.\n");
                    exit(1);
                }

                bytes_read += sizeof(struct tcphdr);

                // Extract fields from the header
                uint16_t src_port = ntohs(tcp_header.src_port);
                uint16_t dest_port = ntohs(tcp_header.dest_port);
                uint32_t seq_num = ntohl(tcp_header.seq_num);
                uint32_t ack_num = ntohl(tcp_header.ack_num);
                uint16_t window = ntohs(tcp_header.window);
                uint16_t urgent = ntohs(tcp_header.urgent_pointer);

                // Extract data offset (upper 4 bits of data_offset_reserved_ns)
                uint8_t data_offset = (tcp_header.data_offset_reserved_ns >> 4) & 0x0F;

                // Extract flags
                uint8_t flags = tcp_header.flags;

                // Print the TCP header fields
                printf("TCP: srcport=%u dstport=%u\n", src_port, dest_port);
                printf("seq=%u ack=%u window=%u urgent=%u\n", seq_num, ack_num, window, urgent);

                // Parse and print the flags
                printf("Flags:");
                if (flags & 0x01) { // Bit 0
                    printf(" FIN");
                }
                if (flags & 0x02) { // Bit 1
                    printf(" SYN"); 
                }
                if (flags & 0x04) { // Bit 2
                    printf(" RST"); 
                }
                if (flags & 0x08) { // Bit 3
                    printf(" PSH");
                }
                if (flags & 0x10) { // Bit 4
                    printf(" ACK");
                }
                if (flags & 0x20) { // Bit 5
                    printf(" URG");
                }
                
                // Calculate total TCP header length
                uint8_t tcp_header_length = data_offset * 4;
                int options_length = tcp_header_length - sizeof(struct tcphdr);

                // Read TCP options if present
                if (options_length > 0) {
                    uint8_t options_buf[40]; // Max TCP options size is 40 Bytes
                    if (fread(options_buf, options_length, 1, file) != 1) {
                        perror("Error reading TCP options.\n");
                        exit(1);
                    }
                    bytes_read += options_length;
                }

                // Calculate total length from IP header
                uint16_t total_length = ntohs(ip_header->tot_len);

                // Caltulate payload length
                int payload_length = total_length - ip_header_len - tcp_header_length;

                // Ensure we do not read beyond the captured length
                int remaining_bytes_in_packet = pkt_header.c_len - bytes_read;
                if (payload_length > remaining_bytes_in_packet) {
                    payload_length = remaining_bytes_in_packet;
                }

                // Read and print the payload
                if (payload_length <= 0) {
                    printf("  No data\n"); // We leave two spaces after flags
                } else {
                    // Read up to K bytes
                    int payload_to_read = (payload_length >= K) ? K : payload_length;
                    uint8_t payload[K];
                    if (fread(payload, payload_to_read, 1, file) != 1) {
                        perror("Error in reading TCP payload.\n");
                        exit(1);
                    }
                    bytes_read += payload_to_read;

                    // Print the payload on the same line as flasgs, leaving two spaces
                    printf("  start of payload:");
                    // For each byte in the initial payload, check if it's printable
                        for (int i = 0; i < payload_to_read; i++) {
                            if (isprint(payload[i])) {
                                printf(" %c", payload[i]);
                            } else {
                                printf(" %02x", payload[i]);
                            }
                        }
                        printf("\n");
                }
            }

        } else if (eth_type == 0x0806) { // Check if the Ethernet frame carries ARP (type == 0x0806)
            struct arphdr arp_header;
            if (fread(&arp_header, sizeof(struct arphdr), 1, file) != 1) {
                perror("Error reading ARP header.\n");
                exit(1);
            }

            bytes_read += sizeof(struct arphdr);

            // Convert fields from network to host byte order
            uint16_t hw_type = ntohs(arp_header.hw_type);
            uint16_t proto_type = ntohs(arp_header.proto_type);
            uint16_t operation = ntohs(arp_header.op);

            // Print ARP header details
            // Hardware type is Ethernet (1) and protocol type is IPv4 (0x0800)
            if (hw_type == 1 && proto_type == 0x0800) {
                printf("ARP: %s htype=%u ptype=%04x hlen=%u plen=%u\n", (operation == 1) ? "request" : "reply", hw_type, proto_type, arp_header.hw_addr_len, arp_header.proto_addr_len);
                printf("snd=");
                print_ip_address(arp_header.sender_ip_addr, 0);
                printf(" tar=");
                print_ip_address(arp_header.target_ip_addr, 0);
                printf("\n");

                if (arp_flag) {
                    printf("sndmac=");
                    print_mac_address(arp_header.sender_hw_addr);
                    printf(" tarmac=");
                    print_mac_address(arp_header.target_hw_addr);
                    printf("\n");
                }
            } else {
                // If hardware type or protocol type doesn't match the expected values, print as unknown
                printf("ARP: unknown type hw_type=%04x proto_type=%04x\n", hw_type, proto_type);
            }
        }

        long bytes_to_seek = pkt_header.c_len - bytes_read;
        if (bytes_to_seek > 0) {
            if (fseek(file, bytes_to_seek, SEEK_CUR) != 0) {
                perror("Error when iterating to next package.\n");
                exit(1);
            }
        }
    }

    return 0;
}

int main(int argc, char *argv[]) {
    int name_flag = 0; // Flag to indicate wether to use domain names
    int arp_flag = 0; // Flag to indicate whether to print detailed ARP fields
    int icmp_verbose = 0;   // Flag for ICMP verbose output

    if (argc < 2 || argc > 4) {
        fprintf(stderr, "Usage: %s <pcap file> [-IP-use-names] [-arpd]\n", argv[0]);
        exit(1);
    }

    // Check if the optional arguments for domain name printing or ARP detail printing are present
    for (int i = 2; i < argc; i++) {
        if (strcmp(argv[i], "-IP-use-names") == 0) {
            name_flag = 1;
        } else if (strcmp(argv[i], "-arpd") == 0) {
            arp_flag = 1;
        } else if (strcmp(argv[i], "-icmpver") == 0) {
            icmp_verbose = 1;
        }
    }

    // Open the file
    FILE *file = fopen(argv[1], "rb");
    if (file == NULL) {
        perror("Error when openning file.\n");
        exit(-1);
    }

    // Parse the PCAP packet header
    if (parse_pcap_packets(file, name_flag, arp_flag, icmp_verbose) != 0) {
        perror("Error parsing packets\n.");
        fclose(file);
        exit(-1);
    }

    // Close the file
    fclose(file);

    return 0;
}