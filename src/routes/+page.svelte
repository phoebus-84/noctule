<script lang="ts">
	import { Camera } from '@capacitor/camera';
	import { goto } from '$app/navigation';
	import FFmpeg from '$lib/nativeHooks/FFmpegPlugin';

	let loading = false;
	let permissionsGranted = true;

	const checkPermissions = async () => {
		let permStatus = await Camera.checkPermissions();
		if (permStatus.photos !== 'granted') permissionsGranted = false;
		if (permStatus.photos === 'prompt') {
			permStatus = await Camera.requestPermissions();
			permissionsGranted = true;
		}
		if (permStatus.photos !== 'granted') {
			throw new Error('User denied permissions!');
		}
	};
	checkPermissions();

	const sessions = async () => {
		return await FFmpeg.getSessions();
	};
</script>

<ion-content fullscreen class="ion-padding">
	<ion-item>
		<ion-text>
			Welcome to Noctule, your go-to video processing app! Easily enhance and modify your videos with a variety of
			filters. Pick a video, choose a filter, adjust parameters, and watch your creativity come to life. Explore the
			world of video effects effortlessly with Noctule
		</ion-text></ion-item
	>
	<ion-list>
		<ion-item>
			<ion-button href="/new" disabled={loading} slot="end">+ new</ion-button>
		</ion-item>
		{#await sessions()}
			sessions list...
		{:then sessions}
			{@const sess = JSON.parse(sessions.sessions)}
			{#if sess.length > 0}
				<ion-item> History: </ion-item>
				{#each JSON.parse(sessions.sessions) as s}
					{@const session = JSON.parse(s)}
					{@const date = new Date(session.endTime)}
					<ion-item>
						<div class="flex flex-col text-xs font-bold">
							<span>
								{date.toLocaleDateString()}
							</span>
							<span>
								{date.toLocaleTimeString()}
							</span>
						</div>
						<div slot="end">
							<ion-fab-button size="small" href={`/session?id=${session.sessionId}`}
								><svg xmlns="http://www.w3.org/2000/svg" class="ionicon" viewBox="0 0 512 512"
									><circle cx="256" cy="256" r="64" /><path
										d="M490.84 238.6c-26.46-40.92-60.79-75.68-99.27-100.53C349 110.55 302 96 255.66 96c-42.52 0-84.33 12.15-124.27 36.11-40.73 24.43-77.63 60.12-109.68 106.07a31.92 31.92 0 00-.64 35.54c26.41 41.33 60.4 76.14 98.28 100.65C162 402 207.9 416 255.66 416c46.71 0 93.81-14.43 136.2-41.72 38.46-24.77 72.72-59.66 99.08-100.92a32.2 32.2 0 00-.1-34.76zM256 352a96 96 0 1196-96 96.11 96.11 0 01-96 96z"
									/></svg
								></ion-fab-button
							>
						</div>
					</ion-item>
				{/each}
			{/if}
		{/await}
	</ion-list>
</ion-content>
