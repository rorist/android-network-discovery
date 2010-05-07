#!/usr/bin/env python

'''
 Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 Licensed under GNU's GPL 2, see README
'''

import os, re, urllib

file = open("../res/raw/services.sql", "w")
url = 'http://nmap.org/svn/nmap-services'

#Download the file
webFile = urllib.urlopen(url)
localFile = open(url.split('/')[-1], 'w')
localFile.write(webFile.read())
webFile.close()
localFile.close()

#Create the file
file.write("BEGIN TRANSACTION;\nCREATE TABLE services (_id INTEGER PRIMARY KEY, service TEXT, port INTEGER);\nCREATE INDEX portIndex ON services (port);\n")
ptn = re.compile("([a-zA-Z0-9\-]+)[\s\t]+([0-9]+)\/tcp")
for line in open("nmap-services"):
  if "/tcp" in line and "unknown" not in line:
    fnd = re.match(ptn, line)
    if fnd:
      file.write("INSERT INTO services ('port', 'service') VALUES (%s, '%s');\n" % (fnd.group(2), fnd.group(1)))

file.write("COMMIT;\n")    
file.close()
os.remove("nmap-services")

