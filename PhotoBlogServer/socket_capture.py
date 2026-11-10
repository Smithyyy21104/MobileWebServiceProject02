
import socket
from pathlib import Path

HOST = "127.0.0.1"
PORT = 8001

OUTPUT_FILE = Path(__file__).with_name("request_capture.txt")

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(1)

    print("Socket Server started")
    print(f"Listening on {HOST}:{PORT}")
    print("Waiting for Android request...")

    connection, address = server.accept()

    with connection:
        connection.settimeout(10)
        request = b""

        while b"\r\n\r\n" not in request:
            data = connection.recv(4096)

            if not data:
                break

            request += data

        request_text = request.decode("iso-8859-1")

        OUTPUT_FILE.write_text(
            request_text,
            encoding="utf-8"
        )

        print("\nAndroid HTTP Request:")
        print(request_text)

        response_body = b"[]"

        response = (
            b"HTTP/1.1 200 OK\r\n"
            b"Content-Type: application/json\r\n"
            b"Content-Length: 2\r\n"
            b"Connection: close\r\n"
            b"\r\n"
            + response_body
        )

        connection.sendall(response)

    print(f"Request saved to: {OUTPUT_FILE}")
    print("Socket Server finished")
