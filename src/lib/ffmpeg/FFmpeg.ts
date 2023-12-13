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

// const glitch = new Filter(
// 	'glitch',
// 	'add some glitches to the video',
// 	'frei0r=filter_name=glitch0r:0.4|0.001|1|1',
// 	null
// );

const motionExtractorParametersSchema = z.object({
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
// Additional Filter Examples
// 0. Grayscale Filter
const scaleParametersSchema = z.object({
	width: z.number(),
	height: z.number()
});
type ScaleParameters = z.infer<typeof scaleParametersSchema>;
const scaleDefaultParameters: ScaleParameters = { width: 640, height: 480 };
const scale = new Filter<ScaleParameters>(
	'scale',
	'resize video dimensions',
	'-vf scale=%s:%s',
	scaleDefaultParameters,
	scaleParametersSchema
);

// 1. Grayscale Filter
const grayscale = new Filter<null>('grayscale', 'convert video to grayscale', '-vf format=gray', null);

// 2. Rotate Filter
const rotateParametersSchema = z.object({
	angle: z.number()
});
type RotateParameters = z.infer<typeof rotateParametersSchema>;
const rotateDefaultParameters: RotateParameters = { angle: 90 };
const rotate = new Filter<RotateParameters>(
	'rotate',
	'rotate video',
	'-vf rotate=%s*PI/180',
	rotateDefaultParameters,
	rotateParametersSchema
);

// 3. Crop Filter
const cropParametersSchema = z.object({
	width: z.number(),
	height: z.number(),
	x: z.number(),
	y: z.number()
});
type CropParameters = z.infer<typeof cropParametersSchema>;
const cropDefaultParameters: CropParameters = { width: 640, height: 480, x: 10, y: 20 };
const crop = new Filter<CropParameters>(
	'crop',
	'crop video',
	'-vf crop=%s:%s:%s:%s',
	cropDefaultParameters,
	cropParametersSchema
);

// 4. Fade In/Out Filter
const fadeInOutParametersSchema = z.object({
	duration: z.number()
});
type FadeInOutParameters = z.infer<typeof fadeInOutParametersSchema>;
const fadeInOutDefaultParameters: FadeInOutParameters = { duration: 5 };
const fadeInOut = new Filter<FadeInOutParameters>(
	'fade_in_out',
	'apply fade in/out effect',
	'-vf "fade=in:0:%s,fade=out:%s:%s"',
	fadeInOutDefaultParameters,
	fadeInOutParametersSchema
);

// 5. Blur Filter
const blurParametersSchema = z.object({
	strength: z.number()
});
type BlurParameters = z.infer<typeof blurParametersSchema>;
const blurDefaultParameters: BlurParameters = { strength: 5 };
const blur = new Filter<BlurParameters>(
	'blur',
	'apply blur effect',
	'-vf "boxblur=%s"',
	blurDefaultParameters,
	blurParametersSchema
);

// 6. Concatenate Filter
const concatenate = new Filter<null>(
	'concatenate',
	'concatenate multiple videos',
	'-filter_complex "[0:v][1:v]concat=n=2:v=1:a=0[out]" -map "[out]"',
	null
);

// 7. Overlay Filter
const overlayParametersSchema = z.object({
	overlayFile: z.string(),
	x: z.number(),
	y: z.number()
});
type OverlayParameters = z.infer<typeof overlayParametersSchema>;
const overlayDefaultParameters: OverlayParameters = { overlayFile: 'overlay.png', x: 10, y: 10 };
const overlay = new Filter<OverlayParameters>(
	'overlay',
	'overlay an image or video',
	'-vf "movie=%s [watermark]; [in][watermark] overlay=%s:%s [out]"',
	overlayDefaultParameters,
	overlayParametersSchema
);

// 8. Denoise Filter
const denoiseParametersSchema = z.object({
	strength: z.number()
});
type DenoiseParameters = z.infer<typeof denoiseParametersSchema>;
const denoiseDefaultParameters: DenoiseParameters = { strength: 0.5 };
const denoise = new Filter<DenoiseParameters>(
	'denoise',
	'reduce noise in video',
	'-vf "hqdn3d=%s:1:1:1"',
	denoiseDefaultParameters,
	denoiseParametersSchema
);

// 9. Reverse Filter
const reverse = new Filter<null>('reverse', 'reverse video playback', '-vf reverse', null);

// 10. Saturation Filter
const saturationParametersSchema = z.object({
	level: z.number()
});
type SaturationParameters = z.infer<typeof saturationParametersSchema>;
const saturationDefaultParameters: SaturationParameters = { level: 1.5 };
const saturation = new Filter<SaturationParameters>(
	'saturation',
	'adjust video saturation',
	'-vf "eq=saturation=%s"',
	saturationDefaultParameters,
	saturationParametersSchema
);

// 11. Sepia Filter
const sepia = new Filter<null>(
	'sepia',
	'apply sepia tone to video',
	'-vf colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131',
	null
);

// 12. Subtitle Filter
const subtitleParametersSchema = z.object({
	text: z.string(),
	font: z.string(),
	fontsize: z.number(),
	x: z.number(),
	y: z.number(),
	color: z.string()
});
type SubtitleParameters = z.infer<typeof subtitleParametersSchema>;
const subtitleDefaultParameters: SubtitleParameters = {
	text: 'Your Subtitle Text',
	font: 'Arial',
	fontsize: 24,
	x: 10,
	y: 10,
	color: 'white'
};
const subtitle = new Filter<SubtitleParameters>(
	'subtitle',
	'add subtitle to video',
	'-vf "drawtext=text=%s:fontfile=%s:fontsize=%s:x=%s:y=%s:fontcolor=%s"',
	subtitleDefaultParameters,
	subtitleParametersSchema
);

// 13. Sharpen Filter
const sharpenParametersSchema = z.object({
	strength: z.number()
});
type SharpenParameters = z.infer<typeof sharpenParametersSchema>;
const sharpenDefaultParameters: SharpenParameters = { strength: 0.5 };
const sharpen = new Filter<SharpenParameters>(
	'sharpen',
	'apply sharpening effect',
	'-vf unsharp=luma_msize_x3=%s:luma_msize_y3=%s',
	sharpenDefaultParameters,
	sharpenParametersSchema
);

// 14. Slow Motion Filter
const slowMotionParametersSchema = z.object({
	speed: z.number()
});
type SlowMotionParameters = z.infer<typeof slowMotionParametersSchema>;
const slowMotionDefaultParameters: SlowMotionParameters = { speed: 0.5 };
const slowMotion = new Filter<SlowMotionParameters>(
	'slow_motion',
	'slow down video playback',
	'-vf setpts=%s*PTS',
	slowMotionDefaultParameters,
	slowMotionParametersSchema
);


// 16. Vignette Filter
const vignetteParametersSchema = z.object({
	strength: z.number()
});
type VignetteParameters = z.infer<typeof vignetteParametersSchema>;
const vignetteDefaultParameters: VignetteParameters = { strength: 0.2 };
const vignette = new Filter<VignetteParameters>(
	'vignette',
	'add vignette effect',
	'-vf "vignette=angle=PI/4:strength=%s"',
	vignetteDefaultParameters,
	vignetteParametersSchema
);

// 17. Deinterlace Filter
const deinterlace = new Filter<null>('deinterlace', 'remove interlacing artifacts', '-vf yadif=1', null);

// 18. Edge Detection Filter
const edgeDetection = new Filter<null>(
	'edge_detection',
	'highlight edges in video',
	'-vf "edgedetect=low=0.1:high=0.4"',
	null
);

// 19. Timecode Overlay Filter
const timecodeOverlay = new Filter<null>(
	'timecode_overlay',
	'add timecode overlay',
	'-vf "drawtext=text=\'%{pts\\:hms}\':rate=30:box=1:boxcolor=black@0.5:fontsize=24"',
	null
);

// 20. Stabilization Filter
const stabilization = new Filter<null>(
	'stabilization',
	'apply video stabilization',
	'-vf vidstabdetect=stepsize=6:shakiness=8:accuracy=15:result=transform_vectors.trf, vidstabtransform=input=transform_vectors.trf',
	null
);

// 11. Brightness Filter
const brightnessParametersSchema = z.object({
    level: z.number(),
});
type BrightnessParameters = z.infer<typeof brightnessParametersSchema>;
const brightnessDefaultParameters: BrightnessParameters = { level: 1.5 };
const brightness = new Filter<BrightnessParameters>('brightness', 'adjust video brightness', '-vf "eq=brightness=%s"', brightnessDefaultParameters, brightnessParametersSchema);

// 12. Saturation Filter (Extended)
const extendedSaturationParametersSchema = z.object({
    level: z.number(),
    contrast: z.number(),
});
type ExtendedSaturationParameters = z.infer<typeof extendedSaturationParametersSchema>;
const extendedSaturationDefaultParameters: ExtendedSaturationParameters = { level: 1.5, contrast: 1.2 };
const extendedSaturation = new Filter<ExtendedSaturationParameters>('extended_saturation', 'adjust video saturation and contrast', '-vf "eq=saturation=%s:contrast=%s"', extendedSaturationDefaultParameters, extendedSaturationParametersSchema);

// 13. Speed Up Filter
const speedUpParametersSchema = z.object({
    speed: z.number(),
});
type SpeedUpParameters = z.infer<typeof speedUpParametersSchema>;
const speedUpDefaultParameters: SpeedUpParameters = { speed: 1.5 };
const speedUp = new Filter<SpeedUpParameters>('speed_up', 'increase video playback speed', '-vf setpts=%s*PTS', speedUpDefaultParameters, speedUpParametersSchema);

// 14. Darken Filter
const darkenParametersSchema = z.object({
    level: z.number(),
});
type DarkenParameters = z.infer<typeof darkenParametersSchema>;
const darkenDefaultParameters: DarkenParameters = { level: 0.8 };
const darken = new Filter<DarkenParameters>('darken', 'darken video', '-vf "eq=brightness=%s"', darkenDefaultParameters, darkenParametersSchema);

// 15. High Pass Filter
const highPassParametersSchema = z.object({
    cutoff: z.number(),
});
type HighPassParameters = z.infer<typeof highPassParametersSchema>;
const highPassDefaultParameters: HighPassParameters = { cutoff: 200 };
const highPass = new Filter<HighPassParameters>('high_pass', 'apply high pass filter', '-vf "highpass=f=%s"', highPassDefaultParameters, highPassParametersSchema);

// 16. Low Pass Filter
const lowPassParametersSchema = z.object({
    cutoff: z.number(),
});
type LowPassParameters = z.infer<typeof lowPassParametersSchema>;
const lowPassDefaultParameters: LowPassParameters = { cutoff: 800 };
const lowPass = new Filter<LowPassParameters>('low_pass', 'apply low pass filter', '-vf "lowpass=f=%s"', lowPassDefaultParameters, lowPassParametersSchema);

// 17. Mirror Horizontal Filter
const mirrorHorizontal = new Filter<null>('mirror_horizontal', 'create a horizontal mirror effect', '-vf hflip', null);

// 18. Mirror Vertical Filter
const mirrorVertical = new Filter<null>('mirror_vertical', 'create a vertical mirror effect', '-vf vflip', null);

// 19. Fade Filter
const fadeParametersSchema = z.object({
    type: z.string(),
});
type FadeParameters = z.infer<typeof fadeParametersSchema>;
const fadeDefaultParameters: FadeParameters = { type: 'in' };
const fade = new Filter<FadeParameters>('fade', 'apply fade in/out effect', '-vf "fade=%s:st=0:d=5"', fadeDefaultParameters, fadeParametersSchema);

// 20. Resize Filter
const resizeParametersSchema = z.object({
    width: z.number(),
    height: z.number(),
});
type ResizeParameters = z.infer<typeof resizeParametersSchema>;
const resizeDefaultParameters: ResizeParameters = { width: 640, height: 480 };
const resize = new Filter<ResizeParameters>('resize', 'resize video dimensions', '-vf scale=%s:%s', resizeDefaultParameters, resizeParametersSchema);

export const filters = [
	mirror,
	negate,
	motionExtractor,
	chromakey,
	grayscale,
	rotate,
	// crop,
	// fadeInOut,
	blur,
	// concatenate,
	// denoise,
	reverse,
	// saturation
	sepia,
	sharpen,
	vignette,
	slowMotion,
	deinterlace,
	edgeDetection,
	timecodeOverlay,
	stabilization,
	brightness,
	extendedSaturation,
	speedUp,
	darken,
	highPass,
	lowPass,
	mirrorHorizontal,
	mirrorVertical,
	fade,
	resize
];