#include <string>
#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <fstream>
#include <filesystem>

using namespace std;


int main(){
      ifstream input("example.txt", ios::in|ios::binary);
      filesystem::path p {"example.txt"};
      auto length = filesystem::file_size(p);
      char *buffer = new char [length];
      input.read (buffer,length);

      ofstream output("test.txt", ios::out|ios::binary);
      output.write(buffer, length);

      delete buffer;

      input.close();
      output.close();
}
