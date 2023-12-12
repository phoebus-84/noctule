import FFmpeg from '$lib/nativeHooks/FFmpegPlugin';
import { z, type ZodSchema } from 'zod';

const destinationFolder = '/storage/emulated/0/Download/';

type Extension = 'mp4' | 'avi' | 'mov';

export class Filter<T> {
	name: string;
	description: string;
	command: string;
	parameters: T;

	constructor(name: string, description: string, command: string, parameters: T) {
		this.name = name;
		this.description = description;
		this.parameters = parameters;
		this.command = command;
	}

	apply = async (input: string, fileType: Extension = 'mp4') => {
		const outputName = `${this.name}-${Date.now().valueOf()}.${fileType}`;
		const res = await FFmpeg.execute({
			// value: `-i /storage/emulated/0/DCIM/Camera/VID_20231206_213225.mp4 -vf ${this.command}  /storage/emulated/0/DCIM/Camera/${destinationName}.mp4`
			// value: `-i ${input} -vf ${this.command} ${destinationFolder}${destinationName}.${fileType}`
			input:input,
			outputName,
			command:this.command
			// value: '-h'
		});
		return { value: res.value, input };
	};
}

const mirror = new Filter(
	'mirror',
	'split and speculate screen',
	'crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right] hstack=inputs=2',
	null
);

const negate = new Filter<null>('negate', 'negative colors filter', 'negate', null);

const glitch = new Filter(
	'glitch',
	'add some glitches to the video',
	'frei0r=filter_name=glitch0r:0.4|0.001|1|1',
	null
);

const motionExtractor = new Filter(
	'motion-etxraction',
	'highlight motion',
	'-filter_complex "[0:v] negate [a]; [a] format=yuva444p,colorchannelmixer=aa=0.5 [b]; [1:v] [b] overlay [out];" -map "[out]"',
	null
)

export const filters = [mirror, negate, glitch];
