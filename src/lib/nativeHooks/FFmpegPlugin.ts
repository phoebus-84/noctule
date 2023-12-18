import { registerPlugin, type PluginCallback } from '@capacitor/core';

export interface FFmpegPlugin {
	execute(options: { input: string; outputName: string; command: string }): Promise<{ logs: string; statistics: any }>;
	getSessions(): Promise<{ sessions: string }>;
}

const FFmpeg = registerPlugin<FFmpegPlugin>('FFmpeg');

export default FFmpeg;
