import { registerPlugin } from '@capacitor/core';

export interface FFmpegPlugin {
	execute(options: { input: string; outputName: string; command: string }): Promise<{ value: string }>;
}

const FFmpeg = registerPlugin<FFmpegPlugin>('FFmpeg');

export default FFmpeg;
