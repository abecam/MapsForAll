The OSM to GPX tool is heavily based on BasicOSMParser, see Readme below. The tool is a standalone graphical tool, with the entry point being com.tgb.mapextractor.view.MainWindow
It is quite rough around the edges, as it main purpose was only to demonstrate the possibility to have map on a certain type of device (without success as these devices are still waiting for their maps). But it works.

Usage
-----

You first need to open an OSM file. You can extract an OSM from OpenStreeMap or other site. As the resulting file will be a contiguous path, and as GPX paths are generally quite limited in how many nodes they can have (1000 on a Suunto Spartan for instance), you probably need to select a small area. But it also strongly depends on how many intersections there are: the path need to pass through all intersection and will often do it more than once (without alternative). The tool is also stochastic and will try many solutions to select the best ones. Trying a "smart" alrogithm to find the best one is in most cases impossible. So it just tries something simple (marking where it passes to privilege where it didn't pass yet) and go at random.


Next you need to select which routes you want to follow. Once you see the map, you can scroll using the arrow keys, zoom using "+" and "-" and paint what to select using the mouse, by pressing the left button. Use the mouse-wheel to make the brush bigger or smaller. The default mode of selection is by point, but it is often better to select by segment, and sometime by way. But ways can be very long. To remove a selection, use the right mouse button. Be careful that the path need to be contiguous, otherwise the tool will probably only convert a part of it - such orphans have been planned but are currently not properly managed.


To start the conversion, press Transform to GPX. It will computes many solutions (400 as the date of writing) and keep 20. It will then visualise the first solution and you can see the other ones by pressing "Select next solution".
The statistic are present (nb of resulting points being the simplest and most important) but currently not shown. But if there are too many points, a degradation might be needed -> "Do the degradation". This will remove points in several passes depending on the angle. Most importantly it won't touch the intersections. 


You can change the parameter of the degradation by changing the 3 numbers. In order: Max angle for degradation, all nodes being separated by an angle above will be untouched, default 15, Incr. of angle, each iteration will allow to remove nodes with a bigger number (so 15+3, then 18+3, ...), default being 3, and the Nb of iterations, default 15.


If the degradation is not good, "Reset to original" restore the selected path.


Once you are happy with the result, don't forget to save the resulting GPX -> File -> Save result as.


BasicOSMParser
==============

Read-me
-------

BasicOSMParser is a collection of Java classes allowing to parse raw OSM XML files.
Then you are able to manipulate those Java objects in your program. You can also export
data as CSV files. This library is very simple to understand. It uses the default Java
SAX parser (org.xml.sax). The application tests use JUnit 4 framework.

Installation (for developers)
------------------------------

In order to use BasicOSMParser, you can download the BasicOSMParser.jar file. Alternatively,
you can put the content of src/main/ folder in the source directory of your project.
Then, add this code in your classes to import the parser :

```
import info.pavie.basicosmparser.controller.*;
import info.pavie.basicosmparser.model.*;
```

Usage
-----

### In another project

Here is a simple example of how to use the parser. You just need to create a new parser
object, and then call the <code>parse</code> method.

```
OSMParser p = new OSMParser();						//Initialization of the parser
File osmFile = new File("/path/to/your/data.osm");	//Create a file object for your OSM XML file

try {

	Map<String,Element> result = p.parse(osmFile);		//Parse OSM data, and put result in a Map object

} catch (IOException | SAXException e) {
	e.printStackTrace();								//Input/output errors management
}
```

The parser returns a <code>Map<String,Element></code> object. This is a collection of key/value pairs.
In this collection, keys are read OSM object identifiers, and values are read OSM objects.
Keys are in a specific format : a letter followed by several digits. The letter corresponds to the kind
of OSM object ('N' for nodes, 'W' for ways, 'R' for relations), and following digits are the OSM numeric ID.
The read OSM objects are represented by Element objects, which can be Node, Way or Relation depending of the
OSM object. You can access the different attributes of objects : ID, user ID, timestamp, version, object tags, ...

You can also directly pass a string which contains XML content, or use an InputSource object.
See the Javadoc of these classes for more details about the available methods.

If you want to get parsed data as several CSV files, use a <code>CSVExporter</code> object :

```
Map<String,Element> result = p.parse(osmFile);
CSVExporter exporter = new CSVExporter();
exporter.export(result, new File("/output/path/for/csv/"));	//Throws IOException if error occurs during writing
```

See the Javadoc of CSVExporter to know more about output CSV format.

### As a data consumer

If you only want to use this parser to create CSV files, you can execute the JAR with the following command :

```
java -jar BasicOSMParser.jar /path/to/data.osm /path/to/output/folder/
```

The command will parse the given OSM XML file, and create the CSV files in the output folder.

License
-------

Copyright 2014 Adrien PAVIE

See LICENSE for complete GPL3 license.

BasicOSMParser is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BasicOSMParser is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BasicOSMParser. If not, see <http://www.gnu.org/licenses/>.
