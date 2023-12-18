<script lang="ts">
	import type { Filter } from '$lib/ffmpeg/FFmpeg';
	import { FilePicker } from '@capawesome/capacitor-file-picker';
	import { Camera } from '@capacitor/camera';
	import { goto } from '$app/navigation';
	import FFmpeg from '$lib/nativeHooks/FFmpegPlugin';

	let video: { name: string | undefined; path: string | undefined; video: any } | undefined;
	let filter: Filter<{ [s: string]: unknown } | null>;
	let err: any;
	let res: string | undefined;
	let loading = false;
	let command: string;
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

	const pickVideos = async () => {
		await FilePicker.pickFiles({
			readData: false,
			multiple: false
		})
			.then((r) => {
				const path = r.files[0].path;
				const name = r.files[0].name;
				if (path && name) video = { path, name, video: r.files[0] };
			})
			.catch((e) => {
				err = e;
			});
	};
	const submit = async () => {
		res = undefined;
		loading = true;
		if (video?.path) {
			command = filter.getCommand();
			res = await filter.apply(video.path).then((r) => (res = r.value));
		}
		loading = false;
	};
	const sessions = async () => {
		return await FFmpeg.getSessions();
	};
</script>

<ion-header translucent={true}>
	<ion-toolbar>
		<ion-buttons slot="start">
			<ion-menu-button />
		</ion-buttons>
		<ion-title>Noctule</ion-title>
	</ion-toolbar>
</ion-header>

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
			{#if video}
				<ion-text>{video.name}</ion-text>
			{:else}
				<div />
			{/if}
			<ion-button on:click={() => goto('/new')} disabled={loading} slot="end">+ new</ion-button>
		</ion-item>
		{#await sessions()}
			sessions list...
		{:then sessions}
			{@const sess = JSON.parse(sessions.sessions)}
			{#if sess.length > 0}
				<ion-item> Sessions: </ion-item>
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
							<ion-button on:click={()=>goto("/session/id")}>see</ion-button>
						</div>
					</ion-item>
				{/each}
			{/if}
		{/await}
	</ion-list>
</ion-content>
