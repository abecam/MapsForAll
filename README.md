# MapsForAll
Push maps on devices that do not support them -> create bitmap tiles or continous GPX.
The goal of this project was to bring maps to devices not supporting them, such as the Suunto Sport.


![Map on Suunto](images/image7.jpg "image_tooltip")


![Corresponding Map](images/image.jpg "image_tooltip")


I have worked on that for quite a while, bought the Fenix 3 to do prototyping, got a prototype running on it using raster map (the user SDK of Garmin is actually very bad with vector data (for some good reasons, not attacking Garmin on that)). The creation part is also somewhere in this project (but not part of the graphical tool).

![alt_text](images/image4.jpg "image_tooltip")


Call me stubborn, I went back to the drawing board, and thought more of having a map on a watch. Also bought an old Oregon 300 for seeing how Garmin does map. And came to the conclusion that we do not need an actual map on a watch, as the screen is way too small, but ony the paths. Actually with a defined route, only the starts of the other paths are needed, to know which direction to go. It’s especially useful in places like forest, when most intersection are with a very small angle. Only with the route I don’t know if I should go left or right, with the drawing on the other path I can.

So what I did is 



* to propose a tool where the user can select which part of a map he would like to have, so only ways, but could be points of the ways, segments or the full way (but all need to be contiguous for being used as a route :( ),
* and to use a deep search algorithm to “walk” on all selected part of paths on a map, in order to create a route that represent a map:



![alt_text](images/image1.jpg "image_tooltip")


It’s called the Route inspection problem, but I addressed it as a computer scientist. Still my algorithm works quite well, and is fast enough to iterate hundreds of time (400 currently, but might be too much), with random choice of direction at each intersection. The paths and intersections are marked as to not go back too many time to an used path (still sometime it is the best solution). At the end only the 10 best solutions are kept:



![alt_text](images/image6.jpg "image_tooltip")


The final number of points is around 2 to 3 time what is initially for this selection. Of course it is a little bit stupid to do that, when the watch could simply “reuse” the code to print the route, and support detached segments.

The next part is actually useful in any way: there the solution can be simplified. My decimation is “smart” in a simple way: it does not touch intersections and, due to the coming several time on the same path, remove always all points at a node (i.e. if a path has been used 4 time, the decimation will always remove the same points for the 4 passages). It removes points based on the angle of the way at the point (remove if less than X), and iterate on the decimation, with the possibility to increase the angle (so next time it will remove more curved parts) :



![alt_text](images/image3.jpg "image_tooltip")



![alt_text](images/image2.jpg "image_tooltip")

