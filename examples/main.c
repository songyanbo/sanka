#include "sanka_header.h"
#include <sanka/examples/WordMap.h>

int main(int argc, char **argv) {
    struct array arr;
    arr.data = argv;
    arr.length = argc;
    WordMap__main(&arr);
    return 0;
}
