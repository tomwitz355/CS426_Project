#undef UNICODE

#define WIN32_LEAN_AND_MEAN

#define byte win_byte_override

#include <Windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <filesystem>
#include <string>
#include <sstream>
#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <fstream>

using namespace std;

#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "12000"


int iResult;

SOCKET ListenSocket = INVALID_SOCKET;
SOCKET ClientSocket = INVALID_SOCKET;


int iSendResult;
char recvbuf[DEFAULT_BUFLEN];
int recvbuflen = DEFAULT_BUFLEN;

DWORD WINAPI sendThread(void* data) {
  cout << "entering" << endl;
  char * t1 = (char*)data;
  soundEngine.playSound(t1);
  cout << "exiting"  << endl;
  return 0;
}

DWORD WINAPI receiveThread(void* data) {
    do{
        ClientSocket = accept(ListenSocket, NULL, NULL);
        if (ClientSocket == INVALID_SOCKET) {
            printf("accept failed with error: %d\n", WSAGetLastError());
            closesocket(ListenSocket);
            WSACleanup();
            return 1;
        }
    }
    while(ClientSocket != ){

        printf("new connection\n");
    }



  return 0;
}

int __cdecl main(void){
    WSADATA wsaData;
    struct addrinfo *result = NULL;
    struct addrinfo hints;

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

    // Accept a client socket
    CreateThread(NULL, 0, receiveThread, NULL, 0, NULL);


    // No longer need server socket
    closesocket(ListenSocket);

    // Receive until the peer shuts down the connection
    do {
        char recvbuf[DEFAULT_BUFLEN] = {0};
        iResult = recv(ClientSocket, recvbuf, recvbuflen, 0);
        if (iResult > 0) {
            // printf("Bytes received: %d\n", iResult);
            // printf("%s\n",recvbuf);

        // Echo the buffer back to the sender
            string temp = string(recvbuf);
            temp.pop_back();
            std::istringstream ss(temp);

            std::string token;
            ifstream input("example.txt", ios::in|ios::binary);
            char *buffer;
            int length = 0;
            bool sent = false;
            while(ss>>token){
                if(token == "file"){
                    filesystem::path p {"example.txt"};
                    int length = filesystem::file_size(p);
                    buffer = new char [length];
                    input.read (buffer,length);
                    iSendResult = send( ClientSocket, buffer , length, 0 );
                    sent = true;
                    printf("Bytes sent: %d\n", length);
                    if (iSendResult == SOCKET_ERROR) {
                        printf("send failed with error: %d\n", WSAGetLastError());
                        closesocket(ClientSocket);
                        WSACleanup();
                        return 1;
                    }
                }
            }
            if(!sent){
                cout << "made it here" << endl;
                char empty[] = "0";
                iSendResult = send( ClientSocket, empty , 1, 0 );
                if (iSendResult == SOCKET_ERROR) {
                    printf("send failed with error: %d\n", WSAGetLastError());
                    closesocket(ClientSocket);
                    WSACleanup();
                    return 1;
                }
            }
            input.close();
            delete buffer;

        }
        else if (iResult == 0)
            printf("Connection closing...\n");
        else  {
            printf("recv failed with error: %d\n", WSAGetLastError());
            closesocket(ClientSocket);
            WSACleanup();
            return 1;
        }

    } while (iResult > 0);

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
