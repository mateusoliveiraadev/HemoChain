CC = gcc
CFLAGS = -Wall -Wextra -std=c11 -g
SRC = src/estoque.c src/fila.c src/pilha.c src/main.c
BIN = rota_vital_estruturas

.PHONY: all test clean

all: $(BIN)

$(BIN): $(SRC)
	$(CC) $(CFLAGS) -o $(BIN) $(SRC)

test: $(BIN)
	./$(BIN)

clean:
	rm -f $(BIN)
