default:
		javac *.java

run:
		java -Xms16384m Test test.wav test.transcript > dump.txt
