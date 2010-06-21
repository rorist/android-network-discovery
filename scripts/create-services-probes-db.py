#!/usr/bin/env python

'''
 Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 Licensed under GNU's GPL 2, see README
'''

import os, re, urllib

file = open("../res/raw/services_probes.sql", "w")
url = 'http://nmap.org/svn/nmap-service-probes'

#Download the file
webFile = urllib.urlopen(url)
localFile = open(url.split('/')[-1], 'w')
localFile.write(webFile.read())
webFile.close()
localFile.close()

#Create the file
file.write("BEGIN TRANSACTION;\nCREATE TABLE services_probes (_id INTEGER PRIMARY KEY, service TEXT, regex TEXT, desc TEXT);\nCREATE INDEX regexIndex ON services_probes (regex);\n")

ptn1 = re.compile("^softmatch ([a-zA-Z0-9\-]+) m\|(.*?)\|[si]? ?[pvihod]?/?(.*?)")
ptn2 = re.compile("^softmatch ([a-zA-Z0-9\-]+) m\/(.*?)\/[si]? ?[pvihod]?/?(.*?)")
#ptn1 = re.compile("^match ([a-zA-Z0-9\-]+) m\|(.*?)\|[si]? ?[pvihod]?/?(.*?)")
#ptn2 = re.compile("^match ([a-zA-Z0-9\-]+) m\/(.*?)\<\/[si]? ?[pvihod]?/?(.*?)")

for line in open("nmap-service-probes"):
  if "match " in line:
    line = line.replace("'", "''") #.replace("\\0", "\\x")
    # First pass
    fnd = re.match(ptn1, line)
    if fnd:
      if fnd.group(2) != None:
        #re.compile("m|"+fnd.group(2)+"|"+fnd.group(3))
        file.write("INSERT INTO services_probes ('service', 'regex', 'desc') VALUES ('%s', '%s', '%s');\n" % (fnd.group(1), fnd.group(2), fnd.group(3)))
    # Second pass
    fnd = re.match(ptn2, line)
    if fnd:
      if fnd.group(2) != None:
        file.write("INSERT INTO services_probes ('service', 'regex', 'desc') VALUES ('%s', '%s', '%s');\n" % (fnd.group(1), fnd.group(2), fnd.group(3)))

file.write("COMMIT;\n")    
file.close()
os.remove("nmap-service-probes")

