#undef UNICODE

#define WIN32_LEAN_AND_MEAN

#define byte win_byte_override

#include <Windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <filesystem>
#include <algorithm>
#include <string>
#include <cctype>
#include <sstream>
#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <fstream>
#include "utility.h"

using namespace std;

#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "12000"


int __cdecl main(void){

// some variables needed for storing data duing the lifetime of the program
    WSADATA wsaData;
    struct addrinfo *result = NULL;
    struct addrinfo hints;
    int iResult;

    SOCKET ListenSocket = INVALID_SOCKET;
    SOCKET ClientSocket = INVALID_SOCKET;

//char pointer to store the received data from the android app
    int iSendResult;
    char recvbuf[DEFAULT_BUFLEN];
    int recvbuflen = DEFAULT_BUFLEN;

    // Initialize Winsock
    iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed with error: %d\n", iResult);
        return 1;
    }

    ZeroMemory(&hints, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_PASSIVE;

    // Resolve the server address and port
    iResult = getaddrinfo(NULL, DEFAULT_PORT, &hints, &result);
    if ( iResult != 0 ) {
        printf("getaddrinfo failed with error: %d\n", iResult);
        WSACleanup();
        return 1;
    }

    // Create a SOCKET for connecting to server
    ListenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
    if (ListenSocket == INVALID_SOCKET) {
        printf("socket failed with error: %ld\n", WSAGetLastError());
        freeaddrinfo(result);
        WSACleanup();
        return 1;
    }

    // Setup the TCP listening socket
    iResult = bind( ListenSocket, result->ai_addr, (int)result->ai_addrlen);
    if (iResult == SOCKET_ERROR) {
        printf("bind failed with error: %d\n", WSAGetLastError());
        freeaddrinfo(result);
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    freeaddrinfo(result);

    iResult = listen(ListenSocket, SOMAXCONN);
    if (iResult == SOCKET_ERROR) {
        printf("listen failed with error: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

/*
main loop, basic flow: every iteration
    - wait for a connection from android app
    - wait for a command to be sent from android app
    - parse the command for a token that matches a defined command
    - execute command if valid
    - send result to the android app
    - disconnect the current connection
*/
    while(true){
        ClientSocket = accept(ListenSocket, NULL, NULL);
        if (ClientSocket == INVALID_SOCKET) {
            printf("accept failed with error: %d\n", WSAGetLastError());
            closesocket(ListenSocket);
            WSACleanup();
            return 1;
        }
        printf("new connection\n");
        char recvbuf[DEFAULT_BUFLEN] = {0};
        iResult = recv(ClientSocket, recvbuf, recvbuflen, 0);
        if (iResult > 0) {
            string temp = string(recvbuf);
            temp.pop_back();
            std::istringstream ss(temp);

            std::string token;
            char *buffer;
            int length = 0;
            bool sent = false;
            while(ss>>token){
// sends a file to the android app.  If the second argument is memo, sends the existing memo over.  Sends an error if the file cannot be found
                if(token == "file"){
                    if(!ss.eof()) {
                        ss>>token;
                        ifstream input;
                        int length;
                        bool found = false;
                        for(auto &p : filesystem::directory_iterator("./")){
                            if(p.path().filename().string().size() >= token.size()){
                                string pathlower = p.path().filename().string().substr(0,token.size());
                                transform(pathlower.begin(), pathlower.end(), pathlower.begin(), ::tolower);
                                transform(token.begin(), token.end(), token.begin(), ::tolower);
                                if(pathlower == token){
                                    length = filesystem::file_size(p);
                                    input = ifstream(p.path().filename().string(), ios::in|ios::binary);
                                    found = true;
                                }
                            }
                        }
                        if(!found){
                            char flag[] = "4";
                            iSendResult = send( ClientSocket, flag , 1, 0 );
                            sent = true;
                        }
                        else{
                            string flag;
                            if(token == "memo"){
                                flag = "6";
                            }
                            else{
                                flag = "2";
                            }
                            iSendResult = send( ClientSocket, flag.c_str() , 1, 0 );
                            buffer = new char [length];
                            input.read (buffer,length);
                            iSendResult = send( ClientSocket, buffer , length, 0 );
                            sent = true;
                            delete buffer;
                            input.close();
                        }
                        if (iSendResult == SOCKET_ERROR) {
                            printf("send failed with error: %d\n", WSAGetLastError());
                            closesocket(ClientSocket);
                            WSACleanup();
                            return 1;
                        }
                    }
                    else{
                        char flag[] = "4";
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                        sent = true;
                    }
                }
// runs the ping python command and sends average latency in miliseconds to the android app
                else if(token == "ping"){
                    string result = GetStdoutFromCommand("python commands/ping.py");
                    char flag[] = "3";
                    sent = true;
                    iSendResult = send( ClientSocket, flag , 1, 0 );
                    iSendResult = send( ClientSocket, result.c_str() , result.size(), 0 );
                }
// runs the clipboard python command and sends the contents of the clipboard to the android app
                else if(token == "clipboard"){
                    string result = GetStdoutFromCommand("julia commands/clipboard.jl");
                    char flag[] = "3";
                    sent = true;
                    iSendResult = send( ClientSocket, flag , 1, 0 );
                    iSendResult = send( ClientSocket, result.c_str() , result.size(), 0 );
                }
// opens an application on the desktop that as long as it is in path.  Sends an error if the application cannot be found
                else if(token == "open"){
                    if(!ss.eof()){
                        ss >> token;
                        int result = system(token.c_str());
                        if(result == -1){
                            char flag[] = "5";
                            sent = true;
                            iSendResult = send( ClientSocket, flag , 1, 0 );
                        }
                        else{
                            char flag[] = "1";
                            sent = true;
                            iSendResult = send( ClientSocket, flag , 1, 0 );
                        }

                    }
                    else{
                        char flag[] = "5";
                        sent = true;
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                    }

                }
// replaces the current memo with a new memo that contains whatever is said after the command
                else if(token == "new"){
                    if(!ss.eof()){
                        ss>>token;
                        if(token == "memo"){
                            if(!ss.eof()){
                                FILE * memo = fopen("memo.txt", "w");
                                fwrite(ss.str().substr(9, ss.str().size()).c_str(), sizeof(char), ss.str().substr(9, ss.str().size()).size(),memo);
                                char newline[] = "\n";
                                fwrite(newline, sizeof(char),1,memo);
                                fclose(memo);
                                char flag[] = "1";
                                sent = true;
                                iSendResult = send( ClientSocket, flag , 1, 0 );
                            }
                            else{
                                char flag[] = "5";
                                sent = true;
                                iSendResult = send( ClientSocket, flag , 1, 0 );
                            }

                        }
                        else{
                            char flag[] = "0";
                            iSendResult = send( ClientSocket, flag , 1, 0 );
                            sent = true;
                        }
                    }
                    else{
                        char flag[] = "0";
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                        sent = true;
                    }
                    break;
                }
// appends to the existing memo whatever is said after the command
                else if(token == "memo"){
                    if(!ss.eof()){
                        FILE * memo = fopen("memo.txt", "a");
                        fwrite(ss.str().substr(5, ss.str().size()).c_str(), sizeof(char),ss.str().substr(5, ss.str().size()).size(),memo);
                        char newline[] = "\n";
                        fwrite(newline, sizeof(char),1,memo);
                        fclose(memo);
                        char flag[] = "1";
                        sent = true;
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                    }
                    else{
                        char flag[] = "5";
                        sent = true;
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                    }
                }
// runs user defined script and sends either success or failure
                else if(token == "run"){
                    if(!ss.eof()){
                        ss>>token;
                        bool found = false;
                        string command;
                        for(auto &p : filesystem::directory_iterator("./commands")){
                            if(p.path().filename().string().size() >= token.size()){
                                string pathlower = p.path().filename().string().substr(0,token.size());
                                transform(pathlower.begin(), pathlower.end(), pathlower.begin(), ::tolower);
                                transform(token.begin(), token.end(), token.begin(), ::tolower);
                                if(pathlower == token){
                                    command = p.path().filename().string();
                                    found = true;
                                }
                            }
                        }
                        if(!found){
                            char flag[] = "4";
                            iSendResult = send( ClientSocket, flag , 1, 0 );
                            sent = true;
                        }
                        else{
                            auto index = command.find('.');
                            string extension = command.substr(index,command.size());
                            string path = "./commands/";
                            path += command;
                            if(extension == ".exe"){
                                int result = system(path.c_str());
                                if(result == -1){
                                    char flag[] = "5";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                                else{
                                    char flag[] = "1";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                            }
                            else if(extension == ".jl"){
                                string julia = "julia " + path;
                                int result = system(julia.c_str());
                                if(result == -1){
                                    char flag[] = "5";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                                else{
                                    char flag[] = "1";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                            }
                            else if(extension == ".py"){
                                string python = "python " + path;
                                int result = system(python.c_str());
                                if(result == -1){
                                    char flag[] = "5";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                                else{
                                    char flag[] = "1";
                                    sent = true;
                                    iSendResult = send( ClientSocket, flag , 1, 0 );
                                }
                            }
                            else{
                                char flag[] = "5";
                                sent = true;
                                iSendResult = send( ClientSocket, flag , 1, 0 );
                            }
                        }
                    }
                    else{
                        char flag[] = "5";
                        sent = true;
                        iSendResult = send( ClientSocket, flag , 1, 0 );
                    }
                }
            }
// if the token cannot be parsed for a valid command, sends invalid command error back to the android app
            if(!sent){
                char flag[] = "0";
                iSendResult = send( ClientSocket, flag , 1, 0 );
                if (iSendResult == SOCKET_ERROR) {
                    printf("send failed with error: %d\n", WSAGetLastError());
                    closesocket(ClientSocket);
                    WSACleanup();
                    return 1;
                }
            }
// closes the current connection
            closesocket(ClientSocket);
        }
// if there is an error with receive, closes down connection
        else  {
            printf("recv failed with error: %d\n", WSAGetLastError());
            closesocket(ClientSocket);
            WSACleanup();
            return 1;
        }
    }



    // No longer need server socket
    // Receive until the peer shuts down the connection
    // shutdown the connection since we're done
    iResult = shutdown(ClientSocket, SD_SEND);
    if (iResult == SOCKET_ERROR) {
        printf("shutdown failed with error: %d\n", WSAGetLastError());
        closesocket(ClientSocket);
        WSACleanup();
        return 1;
    }

    // cleanup
    closesocket(ClientSocket);
    WSACleanup();

    return 0;
}
