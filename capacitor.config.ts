import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
	appId: 'org.dyne.ffnoctuleroom',
	appName: 'FFNoctuleRoom',
	webDir: 'build',
	server: {
		androidScheme: 'https',
		cleartext: true
	}
};

export default config;
