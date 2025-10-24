# Create boat graphics for the 2D race viewer

This page describes how to create the SVG graphics for different boat classes which can be used by the 2D race viewer to draw boat graphics directly to an HTML Canvas.

## Inkscape

We are using the free Inkscape (https://inkscape.org/) as the tool to create and edit SVG graphics.
However, there are some settings you have to change in Inkscape before you should start working with it.

### Inkscape preferences
In the 'SVG output' section of the Inkscape preference you need to change the number precision to '3' and the minimum exponent to '-3'. Otherwise you get very long numbers (e.g. 3.569999999) when saving the SVG file.

### Coordinate system origin
One problem with using Inkscape is that the origin of the drawing area (and the ruler tools) is in the lower left corner whereas the origin of a SVG graphics is the upper left corner. To avoid confusion with the coordinates later on we recommend to put the graphics into the upper left corner. This makes it a little more complicated to work with the ruler, but ensures that the coordinates are correct in the canvas.

### Scale
To draw the boats later on the map with the right scale/size we also have to be clear about the used 'scale' when drawing the graphics. Right now the used scale is 1px = 1cm. So e.g. a 49er boat with the size of 4.90 x 2.50 meters would use a SVG document with the size 490px x 250px.

## Conversion of SVG graphics into a Canvas drawing commands

(Probably outdated: You can use the online tool 'SVG to HTML5 Canvas Converter' https://www.professorcloud.com/svg-to-canvas/ to convert the SVG graphics to a list of drawing commands. So for example a SVG line<br/>
&lt;line x1="1050" y1="651" x2="1050" y2="49" id="line75" /><br/>
will be converted to something like<br/>
ctx.beginPath();<br/>
ctx.moveTo(1050,651);<br/>
ctx.lineTo(1050,49);<br/>
ctx.fill();<br/>
ctx.stroke();<br/>
This is actually javascript code but can almost directly be used as Java code as well.)

Update: according to [https://www.quora.com/How-do-I-convert-SVG-to-canvas](https://www.quora.com/How-do-I-convert-SVG-to-canvas) Inkscape meanwhile support a "Save as HTML5 canvas" option. This should probably do the trick, as professorcloud.com has long disappeared.

## Pitfalls
Sometimes it happens during the editing of the SVG that you accidentally create transformations for the whole graphics...