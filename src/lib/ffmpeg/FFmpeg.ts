import FFmpeg from '$lib/nativeHooks/FFmpegPlugin';
import { z } from 'zod';

type Extension = 'mp4' | 'avi' | 'mov';

function parseCommand(format: string, ...args: any[]): string {
	return format.replace(/%s/g, () => args.shift());
}

export class Filter<T extends { [s: string]: unknown } | null> {
	name: string;
	description: string;
	command: string;
	schema: z.AnyZodObject | undefined;
	parameters: T;

	constructor(name: string, description: string, command: string, parameters: T, schema?: z.AnyZodObject) {
		this.name = name;
		this.description = description;
		this.parameters = parameters;
		this.command = command;
		this.schema = schema;
	}

	apply = async (input: string, fileType: Extension = 'mp4') => {
		const outputName = `${this.name}-${Date.now().valueOf()}.${fileType}`;
		const res = await FFmpeg.execute({
			input: input,
			outputName,
			command: this.getCommand()
		});
		return { value: res.value, input };
	};

	getCommand = () => (this.parameters ? parseCommand(this.command, Object.values(this.parameters)) : this.command);

	setParameters = (parameters: T) => {
		this.parameters = parameters;
	};
}

const mirror = new Filter(
	'mirror',
	'split and speculate screen',
	'-vf "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right] hstack=inputs=2"',
	null
);

const negate = new Filter<null>('negate', 'negative colors filter', '-vf negate', null);

const glitch = new Filter(
	'glitch',
	'add some glitches to the video',
	'frei0r=filter_name=glitch0r:0.4|0.001|1|1',
	null
);
export const motionExtractorParametersSchema = z.object({
	startFrame: z.number()
});

type MotionExtractorParameters = z.infer<typeof motionExtractorParametersSchema>;

const motionExtractor = new Filter<MotionExtractorParameters>(
	'motion-etxraction',
	'highlight motion',
	'-filter_complex "[0:v] split  [a][c]; [a] negate, trim=start_frame=%s,setpts=N/FR/TB, format=yuva444p,colorchannelmixer=aa=0.5 [b]; [c] [b] overlay;"',
	{ startFrame: 20 },
	motionExtractorParametersSchema
);

const chromakeyParametersSchema = z.object({
	color: z.string()
});

type ChromakeyParameters = z.infer<typeof chromakeyParametersSchema>;
const chromakeyDefaultParameters: ChromakeyParameters = { color: 'green' };
const chromakey = new Filter(
	'chromakey',
	'transparent pixel for color',
	'-vf chromakey=%s',
	chromakeyDefaultParameters,
	chromakeyParametersSchema
);

export const filters = [mirror, negate, glitch, motionExtractor, chromakey];
