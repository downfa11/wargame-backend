#include "Lobby.h"
#include "GameManager.h"

void HandleClient(SOCKET clientSocket);

SOCKET serverSocket;
SOCKET clientSocket;
string web_address = "127.0.0.1:80";
//"3.35.18.37:80";

mutex mLock;

SOCKET WebServerConnect() {
    serverSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSocket == INVALID_SOCKET) {
        cerr << "Failed to create server socket." << endl;
        return NULL;
    }

    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_addr.s_addr = INADDR_ANY;
    serverAddr.sin_port = htons(8080);

    if (bind(serverSocket, (sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
        cerr << "Failed to bind server socket." << endl;
        closesocket(serverSocket);
        return NULL;
    }

    if (listen(serverSocket, SOMAXCONN) == SOCKET_ERROR) {
        cerr << "Failed to listen on server socket." << endl;
        closesocket(serverSocket);
        return NULL;
    }

    return serverSocket;
}

void Lobby::start() {

    serverSocket = WebServerConnect();

    while (true) {
        if (serverSocket == INVALID_SOCKET)
            break;

        sockaddr_in clientAddr;
        int clientAddrSize = sizeof(clientAddr);
        clientSocket = accept(serverSocket, (sockaddr*)&clientAddr, &clientAddrSize);
        if (clientSocket == INVALID_SOCKET) {
            cerr << "Failed to accept client connection." << endl;
            continue;
        }

        cout << "Connected Web Server." << endl;
        HandleClient(clientSocket);
    }

    closesocket(serverSocket);
}

void HandleClient(SOCKET clientSocket) {
    char buffer[1024];
    int bytesRead = recv(clientSocket, buffer, sizeof(buffer), 0);
    lock_guard<mutex> lock(mLock);
    if (bytesRead <= 0) {
        cerr << "Failed to read client request." << endl;
        closesocket(clientSocket);
        return;
    }

    else {
        buffer[bytesRead] = '\0';
        string request(buffer);

        string httpMethod;
        if (request.find("GET ") == 0)
            httpMethod = "GET";
  
        else if (request.find("POST ") == 0)
            httpMethod = "POST";
        else {
            cerr << "Unknown HTTP method" << endl;
            closesocket(clientSocket);
            return;
        }

        size_t pathStart=0;
        if (httpMethod =="GET")
            pathStart = request.find("GET ") + 4;
        else if(httpMethod=="POST")
            pathStart = request.find("POST ") + 5;

        size_t pathEnd = request.find(" HTTP/1.1");
        string path = request.substr(pathStart, pathEnd - pathStart);

        size_t paramsStart = path.find("?");
        string params = "";
        if (paramsStart != string::npos) {
            params = path.substr(paramsStart + 1);
            path = path.substr(0, paramsStart);
        }

        //cout << "path:" << path << endl;
        //cout << "params:" << params << endl;

        string response;
        if (path == "/hello" && httpMethod == "GET")
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello, World!";

        if (path == "/alreadyInGame" && httpMethod == "GET")
        {
            // userCode를 받아서 GameManager::auth_data에 있는지 확인 후 bool 값을 전송한다.

            size_t startPos = request.find("/alreadyInGame?userCode=") + strlen("/alreadyInGame?userCode=");
            size_t endPos = request.find(" HTTP/1.1");
            std::string userCode = request.substr(startPos, endPos - startPos);

            bool isGame = false;
            for (auto& room : GameManager::auth_data) {
                for (auto& inst : room.user_data) {
                    if (inst.user_code == userCode)
                        isGame = true;
                    else continue;
                }
            }
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n" + string(isGame ? "true" : "false");
        }


        else if (path == "/match" && httpMethod == "POST") {
            size_t jsonStart = request.find("{");
            size_t jsonEnd = request.find_last_of("}");
            if (jsonStart != string::npos && jsonEnd != string::npos) {
                string jsonData = request.substr(jsonStart, jsonEnd - jsonStart + 1);

                try {

                    int room = 0;
                    int channel = 0;
         
                    size_t userStart = jsonData.find("\"user_info_list\": ");

                    if (userStart == string::npos)
                        return;

                    roomData roominfo;
                    roominfo.channel = 0;
                    roominfo.room = 0;

                    // user_info_list 파싱
                    size_t userListStart = jsonData.find("[", userStart);
                    size_t userListEnd = jsonData.find("]", userListStart);

                    string userListData = jsonData.substr(userListStart, userListEnd - userListStart);

                    size_t userStartIndex = 0;
                    size_t userEndIndex = 0;

                    while ((userEndIndex = userListData.find("{", userStartIndex)) != string::npos) {
                        userStartIndex = userEndIndex + 1;

                        size_t userEndBraceIndex = userListData.find("}", userStartIndex);
                        if (userEndBraceIndex == string::npos)
                            return;

                        string userObject = userListData.substr(userStartIndex, userEndBraceIndex - userStartIndex);
                        
                        UserData userData;

                        size_t userNameStart = userObject.find("\"user_name\":") + 14;
                        size_t userIdEnd = userObject.find("\"", userNameStart);
                        userData.user_name = userObject.substr(userNameStart, userIdEnd - userNameStart);

                        size_t userCodeStart = userObject.find("\"user_code\":") + 14;
                        size_t userCodeEnd = userObject.find("\"", userCodeStart);
                        userData.user_code = userObject.substr(userCodeStart, userCodeEnd - userCodeStart);

                        roominfo.user_data.push_back(userData);
                        GameManager::auth_data.push_back(roominfo);
                    }

                }
                catch (const exception& e)
                {
                    cerr << "Failed to parse JSON data: " << e.what() << endl;
                }
            }
            else cerr << "JSON data not found in POST request." << endl;

            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nIs it Okay?";
        }
        else if (path == "/custom")
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nCustom Response: " + params;
        else 
            response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found";

        send(clientSocket, response.c_str(), response.size(), 0);
    }
    closesocket(clientSocket);
}

void Lobby::SendToWebServer(string path, string request, size_t length) {

    string postRequest = "POST " + path + 
        " HTTP/1.1\r\nHost: "+ web_address + "\r\nContent-Type: application/json\r\nContent-Length: "
        + to_string(length) + "\r\n\r\n" + request;

    if (send(clientSocket, postRequest.c_str(), postRequest.length(), 0) == SOCKET_ERROR)
    {
        cerr << "Failed to send data to the web server." << endl;
        return;
    }
    else cout << "Data send to WebServer successfully." << endl;

    cout << request << endl;
}