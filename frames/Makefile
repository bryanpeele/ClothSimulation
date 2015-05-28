CLASSPATH=.:jogl.jar:gluegen-rt.jar

build:
	javac -classpath $(CLASSPATH) cs5643/particles/*.java

run:
	java -Xmx1000m -classpath $(CLASSPATH) cs5643.particles.ParticleSystemBuilder

doc:
	javadoc -d doc -classpath $(CLASSPATH) cs5643.particles

clean:
	rm -f *~ cs5643/particles/*~ cs5643/particles/*.class
