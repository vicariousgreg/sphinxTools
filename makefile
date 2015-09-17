default:
		javac *.java

run:
		java -Xms16384m Test /audio/transcripts/audio/quick4.wav /audio/transcripts/human/quick4.transcript > dump.txt
