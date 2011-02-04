#!/usr/bin/env python

'''
 Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 Licensed under GNU's GPL 2, see README
'''

import sqlite3, os, urllib, re

db = '../res/raw/probes.db'
url = 'http://nmap.org/svn/nmap-service-probes'

#Download the file
webFile = urllib.urlopen(url)
localFile = open(url.split('/')[-1], 'w')
localFile.write(webFile.read())
webFile.close()
localFile.close()

#Create the DB
try:
  os.remove(db)
except OSError, err:
  print err
conn = sqlite3.connect(db)
c = conn.cursor()
c.execute("CREATE TABLE probes (_id INTEGER PRIMARY KEY, service TEXT, regex TEXT, desc TEXT);")
c.execute("CREATE INDEX regexIndex ON probes (regex);")

ptn1 = re.compile("^(soft)?match (ftp|sftp|ssh|telnet|http) m\|(.*?)\|[si]? ?[pvihod]?/?(.*?)")
ptn2 = re.compile("^(soft)?match (ftp|sftp|ssh|telnet|http) m\/(.*?)\/[si]? ?[pvihod]?/?(.*?)")

for line in open("nmap-service-probes"):
  if "Probes" in line:
    print line
  elif "match " in line:
    line = line.replace("'", "''").replace("\\0", "\\\\0")
    # First pass
    fnd = re.match(ptn1, line)
    if fnd:
      if fnd.group(3) != None:
        try:
          c.execute("INSERT INTO probes ('service', 'regex', 'desc') VALUES ('%s', '%s', '%s');" % (fnd.group(2), fnd.group(3), fnd.group(4)))
        except sqlite3.OperationalError, err:
          print err 
    # Second pass
    fnd = re.match(ptn2, line)
    if fnd:
      if fnd.group(3) != None:
        try:
          c.execute("INSERT INTO probes ('service', 'regex', 'desc') VALUES ('%s', '%s', '%s');\n" % (fnd.group(2), fnd.group(3), fnd.group(4)))
        except sqlite3.OperationalError, err:
          print err 

conn.commit()
c.close()
os.remove("nmap-service-probes")

