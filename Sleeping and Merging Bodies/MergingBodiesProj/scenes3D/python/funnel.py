import xml.etree.ElementTree as ET
import xml.dom.minidom as md

from modules.box import Box
from modules.stand import Stand
from modules.composite import CompositeBody
from modules.exportXML import export

root = ET.Element('root')

# Plane
plane = ET.SubElement(root, 'body')
plane.set('type','plane')
plane.set('p','0 0 0')
plane.set('n','0. 1. 0.0')
plane.set('name','plane')
fric = ET.SubElement(plane, 'friction')
fric.text = "0.01"

# Bodies pile
Box(root, name='box', position="0 10 0", orientation="0 0 1 0", dim="1 1 1", pinned="true")

# chariot
composite = CompositeBody(root, obj="data/funnel.obj", scale="2", name="funnel", position="0. 50. 0.", velocity="0. 0. 0.")
composite.addBox(name='wall1', position="0. 0. -9.", orientation="-1 0 0 0.5", dim='28 22 1')
composite.addBox(name='wall3', position="0. 0. 9.", orientation="1 0 0 0.5", dim='28 22 1')
composite.addBox(name='wall2', position="-9. 0. 0.", orientation="0 0 1 0.5", dim='1 22 28')
composite.addBox(name='wall4', position="9. 0. 0.", orientation="0 0 -1 0.5", dim='1 22 28')

Stand(root, body2="funnel", dimBody2="26 20 26", ybody2="74", width="40", length="100")

export(root, "../funnel.xml")
