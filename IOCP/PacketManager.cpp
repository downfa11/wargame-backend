#include "PacketManager.h"
#include "base.h"

void PacketManger::Send(int cli_sock, int number,void* data, int size)
{
	Header header;
	header.number = number;
	header.size = size;

	char bt_header[sizeof(Header)];
	char* bt_result = new char[sizeof(Header) + size];
	char* bt_data = new char[size];

	//printf("%d size �Ҵ�", sizeof(Header) + size);

	fill_n(bt_result, sizeof(Header) + size, 0);

	memcpy(&bt_header, &header, sizeof(Header));
	memcpy(bt_data, data, size);

	for (int i = 0; i < sizeof(Header); i++)
	{
		bt_result[i] = bt_header[i];
	}

	for (int i = sizeof(Header); i < sizeof(Header) + size; i++)
	{
		bt_result[i] = bt_data[i - sizeof(Header)];
	}
	delete[] bt_data;

	LPPER_IO_DATA ioInfo = new PER_IO_DATA;
	memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));
	ioInfo->wsaBuf.buf = bt_result;
	ioInfo->wsaBuf.len = sizeof(Header) + size;
	ioInfo->rwMode = WRITE;

	WSASend(cli_sock, &ioInfo->wsaBuf, 1, NULL, 0, &(ioInfo->overlapped), NULL);
}

void PacketManger::Send(int cli_sock, int number)
{
	Header header;
	header.number = number;
	header.size = 0;

	char* bt_header = new char[sizeof(Header)];

	memcpy(bt_header, &header, sizeof(Header));

	LPPER_IO_DATA ioInfo = new PER_IO_DATA;
	memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));
	ioInfo->wsaBuf.buf = bt_header;
	ioInfo->wsaBuf.len = sizeof(Header);
	ioInfo->rwMode = WRITE;

	WSASend(cli_sock, &ioInfo->wsaBuf, 1, NULL, 0, &(ioInfo->overlapped), NULL);
}