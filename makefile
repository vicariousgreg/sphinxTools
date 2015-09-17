default:
		javac SpeechTools.java Alignment.java SenoneDump.java Test.java TranscriptAlignment.java WordAlignment.java FrameAlignment.java Segment.java Segmenter.java

run:
		java -Xms16384m Test /audio/transcripts/audio/quick4.wav /audio/transcripts/human/quick4.transcript > dump.txt

