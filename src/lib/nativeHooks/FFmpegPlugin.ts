import { registerPlugin, type PluginCallback } from '@capacitor/core';

export interface FFmpegPlugin {
	execute(options: { input: string; outputName: string; command: string }): Promise<{ value: string }>;
	// getStatistics(options: { sessionId: number }, callback: PluginCallback): Promise<string>;
}

const FFmpeg = registerPlugin<FFmpegPlugin>('FFmpeg');

export default FFmpeg;
