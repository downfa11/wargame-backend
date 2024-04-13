#include "PacketManager.h"
#include "base.h"

void PacketManger::Send(int cli_sock, int number,void* data, int size)
{
	nsHeader header;
	header.number = number;
	header.size = size;

	char bt_header[sizeof(nsHeader)];
	char* bt_result = new char[sizeof(nsHeader) + size];
	char* bt_data = new char[size];

	fill_n(bt_result, sizeof(nsHeader) + size, 0);

	memcpy(&bt_header, &header, sizeof(nsHeader));
	memcpy(bt_data, data, size);

	for (int i = 0; i < sizeof(nsHeader); i++)
	{
		bt_result[i] = bt_header[i];
	}

	for (int i = sizeof(nsHeader); i < sizeof(nsHeader) + size; i++)
	{
		bt_result[i] = bt_data[i - sizeof(nsHeader)];
	}
	delete[] bt_data;

	LPPER_IO_DATA ioInfo = new PER_IO_DATA;
	memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));
	ioInfo->wsaBuf.buf = bt_result;
	ioInfo->wsaBuf.len = sizeof(nsHeader) + size;
	ioInfo->rwMode = WRITE;

	WSASend(cli_sock, &ioInfo->wsaBuf, 1, NULL, 0, &(ioInfo->overlapped), NULL);
}

void PacketManger::Send(int cli_sock, int number)
{
	nsHeader header;
	header.number = number;
	header.size = 0;

	char* bt_header = new char[sizeof(nsHeader)];

	memcpy(bt_header, &header, sizeof(nsHeader));

	LPPER_IO_DATA ioInfo = new PER_IO_DATA;
	memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));
	ioInfo->wsaBuf.buf = bt_header;
	ioInfo->wsaBuf.len = sizeof(nsHeader);
	ioInfo->rwMode = WRITE;

	WSASend(cli_sock, &ioInfo->wsaBuf, 1, NULL, 0, &(ioInfo->overlapped), NULL);
}