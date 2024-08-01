Just a little private project

Usage:

```bash
java -jar RDS2Audacity.jar [input] [output] <keywords...>
Use - as input to read from standard input
Use - as output to write to standard output
```

# Background - Why this project exists?
This project is capable of converting RDS data outputted by [redsea](https://github.com/windytan/redsea/) in JSON format, in the form of one JSON object per line,  
and converts it to the label format used by [Audacity](https://www.audacityteam.org/).  
It allows me to (roughly) detect, when what song is played in my local FM broadcast stations.  
I use it to easily split recording of FM transmission into separate named music tracks.
