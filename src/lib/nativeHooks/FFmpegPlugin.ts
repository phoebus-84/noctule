import { registerPlugin, type PluginCallback } from '@capacitor/core';

export interface FFmpegPlugin {
	execute(options: { input: string; outputName: string; command: string }): Promise<{ logs: string; statistics: any }>;
	getSessions(): Promise<{ sessions: string }>;
	getSession(options: {
		sessionId: number;
	}): Promise<{
		logs: string;
		command: string;
		arguments: string[];
		startTime: number;
		endTime: number;
		duration: number;
	}>;
}

const FFmpeg = registerPlugin<FFmpegPlugin>('FFmpeg');

export default FFmpeg;
