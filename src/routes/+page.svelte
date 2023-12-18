<script lang="ts">
	import { Filter, filters } from '$lib/ffmpeg/FFmpeg';
	import ParametersFields from '$lib/forms/ParametersFields.svelte';
	import Logs from '$lib/logs/Logs.svelte';
	import { FilePicker } from '@capawesome/capacitor-file-picker';
	import { Camera } from '@capacitor/camera'

	let video: { name: string | undefined; path: string | undefined; video: any } | undefined;
	let filter: Filter<{ [s: string]: unknown } | null>;
	let err: any;
	let res: string | undefined;
	let loading = false;
	let command: string;
	let permissionsGranted = true

	const checkPermissions = async () => {
		let permStatus = await Camera.checkPermissions();
		if if (permStatus.photos !== 'granted') permissionsGranted = false;
		if (permStatus.photos === 'prompt') {
			permStatus = await Camera.requestPermissions();
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
</script>

<ion-header translucent={true}>
	<ion-toolbar>
		<ion-buttons slot="start">
			<ion-menu-button />
		</ion-buttons>
		<ion-title>FFNoctuleRoom</ion-title>
	</ion-toolbar>
</ion-header>

{#if !permissionsGranted}
	<ion-text color="danger">You should allow this app to access photo and video in order to use it.</ion-text>
{:else}
	<ion-content fullscreen class="ion-padding">
		<ion-item>
			<ion-text>
				Welcome to FFNoctuleRoom, your go-to video processing app! Easily enhance and modify your videos with a variety
				of filters. Pick a video, choose a filter, adjust parameters, and watch your creativity come to life. Explore
				the world of video effects effortlessly with FFNoctuleRoom {p}
			</ion-text>
		</ion-item>
		<ion-list>
			<ion-item>
				{#if video}
					<ion-text>{video.name}</ion-text>
				{:else}
					<div />
				{/if}
				<ion-button on:click={pickVideos} disabled={loading} slot="end">pick a video</ion-button>
			</ion-item>
			<ion-item>
				<ion-select
					label="Choose a filter"
					disabled={loading}
					placeholder="..."
					on:ionChange={(e) => {
						filter = e.detail.value;
					}}
				>
					{#each filters as filter}
						<ion-select-option value={filter}>{filter.name}</ion-select-option>
					{/each}
				</ion-select>
			</ion-item>
			{#if video?.path && filter?.parameters}
				<ParametersFields {filter} {submit} {loading} />
			{:else if video?.path && filter}
				<ion-item>
					<ion-button slot="end" on:click={submit} disabled={loading}>apply</ion-button>
				</ion-item>
			{/if}
			{#if loading}
				<ion-spinner name="lines-sharp" />
			{/if}
			{#if command}
				<ion-item>
					{command}
				</ion-item>
			{/if}
			{#if res}
				<ion-item>
					<Logs logString={res} />
				</ion-item>
			{/if}
		</ion-list>
	</ion-content>
{/if}
