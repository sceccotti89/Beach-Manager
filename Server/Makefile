
# compilatore
CC = gcc
CFLAGS = -O3 -pedantic -Wall -Wextra -Wformat=2 -ggdb

# directory in cui si trovano le librerie
LIBDIR = ./lib
# opzione di linking
LIBS = -L $(LIBDIR)

# nome libreria per gestire l'applicazione
LIBNAME1 = libdata.a
# oggetti libreria $(LIBNAME1)
objects1 = hash.o threadPool.o dataThread.o sqlQuery.o

# nome libreria socket di rete
LIBNAME2 = libsock.a
# oggetti libreria $(LIBNAME2)
objects2 = comsock.o

# nome libreria criptaggio dati
LIBNAME3 = libcripto.a
# oggetti libreria $(LIBNAME3)
objects3 = cripto.o sha2.o

# nome eseguibili terzo frammento
exeserv = server

.PHONY: clean backup resume_backup lib1 lib2 lib3 launcher

# creazione libreria gestione del gioco
lib1: $(objects1)
	-rm  -f $(LIBDIR)/$(LIBNAME1)
	ar -r $(LIBNAME1) $(objects1)
	cp $(LIBNAME1) $(LIBDIR)

# creazione libreria socket di rete
lib2: $(objects2)
	-rm  -f $(LIBDIR)/$(LIBNAME2)
	ar -r $(LIBNAME2) $(objects2)
	cp $(LIBNAME2) $(LIBDIR)

# creazione libreria per criptare i dati
lib3: $(objects3)
	-rm  -f $(LIBDIR)/$(LIBNAME3)
	ar -r $(LIBNAME3) $(objects3)
	cp $(LIBNAME3) $(LIBDIR)

# determina l'architettura della macchina (32 o 64 bit) 
ARCH=$(shell uname -m)

# opzioni libreria dinamica mysl
MYSQL_DYNAMIC_OPT=-lmysqlclient

# opzioni libreria statica mysql
ifeq ($(ARCH), i686)                   # 32 bit
	MYSQL_STATIC_OPT = `mysql_config --cflags --libs` libmysqlclient.a -lz -ldl
else                                   # 64 bit
	MYSQL_STATIC_OPT = `mysql_config --cflags --libs` libmysqlclient.a -lz -ldl
endif

$(exeserv): server.o
	$(CC) -D_REENTRANT -o $@ $^ $(LIBS) -ldata -lsock -lcripto $(MYSQL_STATIC_OPT) -lpthread -lm

server.o: server.c sqlQuery.h comsock.h errors.h dataThread.h cripto.h threadPool.h hash.h
	$(CC) $(CFLAGS) -c $<

# esegue il backup di tutti i file presenti
backup:
	cp *.c *h Makefile ./backup
	date > ./backup/last_modified.txt
	chmod 700 backup

# ripristina il backup dei dati
resume_backup:
	cp -f ./backup/*.c ./backup/*h .

#ripulisce  l'ambiente
clean:
	-rm -f *.o *.gch *.a *~ ./core ./lib/*.a

launcher: 
	make clean
	cp ./lib/mysql/libmysqlclient.a .
	make lib1
	make lib2
	make lib3
	make $(exeserv)
	-rm -f ./.mtrace.log
	#MALLOC_TRACE=./.mtrace.log ./$(exeserv)
	#mtrace ./$(exeserv) ./.mtrace.log
	#valgrind --track-origins=yes --leak-check=full --show-leak-kinds=all ./$(exeserv)
	valgrind --track-origins=yes ./$(exeserv)
	#./$(exeserv)
