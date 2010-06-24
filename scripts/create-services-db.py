#!/usr/bin/env python

'''
 Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 Licensed under GNU's GPL 2, see README
'''

import sqlite3, os, urllib, re

db = '../res/raw/services.db'
url = 'http://nmap.org/svn/nmap-services'

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
c.execute("CREATE TABLE services (_id INTEGER PRIMARY KEY, service TEXT, port INTEGER);")
c.execute("CREATE INDEX portIndex ON services (port);")

ptn = re.compile("([a-zA-Z0-9\-]+)[\s\t]+([0-9]+)\/tcp")
for line in open("nmap-services"):
  if "/tcp" in line and "unknown" not in line:
    fnd = re.match(ptn, line)
    if fnd:
      try:
        c.execute("INSERT INTO services ('port', 'service') VALUES (%s, '%s');" % (fnd.group(2), fnd.group(1)))
      except sqlite3.OperationalError, err:
        print err 

conn.commit()
c.close()
os.remove("nmap-services")

