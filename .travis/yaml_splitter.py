#!/usr/bin/env python
import yaml

with open('./triplea_maps.yaml', 'r') as stream:
	try:
		for map in yaml.safe_load(stream):
			for key, value in map.items():
				if isinstance(value, str):
					map[key] = value.strip();
			map['slug'] = map['mapName'].lower().replace(' ', '-');
			with open('./website/_maps/' + map['slug'] + '.html', 'w+') as mapFile:
				mapFile.write('---\n');
				map['title'] = map['mapName'] + ' | TripleA Map';
				description = map.pop('description', '');
				map['dlurl'] = map.pop('url', '');
				yaml.dump(map, mapFile, default_flow_style=False);
				mapFile.write('---\n');
				mapFile.write(description);
				mapFile.write('\n');
	except yaml.YAMLError as exc:
		print(exc);
