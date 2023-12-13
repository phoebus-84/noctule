<script lang="ts">
	import { Filter, filters } from '$lib/ffmpeg/FFmpeg';
	import ParametersFields from '$lib/forms/ParametersFields.svelte';
	import Logs from '$lib/logs/Logs.svelte';
	import { FilePicker } from '@capawesome/capacitor-file-picker';

	let video: { name: string | undefined; path: string | undefined; video: any } | undefined;
	let filter: Filter<{ [s: string]: unknown } | null>;
	let err: any;
	let res: string;
	let loading = false;
	let command: string;
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

<ion-content fullscreen class="ion-padding">
	<!-- <ion-card>
		<ion-card-content>
			Frei0r is a minimalistic plugin API for video effects. The main emphasis is on simplicity for an API that will
			round up the most common video effects into simple filters, sources and mixers that can be controlled by
			parameters. It’s our hope that this way these simple effects can be shared between many applications, avoiding
			their reimplementation by different projects.
		</ion-card-content>
	</ion-card> -->
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
