<script lang="ts">
	import { Filter, filters } from '$lib/ffmpeg/FFmpeg';
	import Logs from '$lib/logs/Logs.svelte';
	import { FilePicker } from '@capawesome/capacitor-file-picker';

	let video: { name: string | undefined; path: string | undefined; video: any } | undefined;
	let filter: Filter<{ [s: string]: unknown } | null>;
	let err: any;
	let res = '';
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
		if (video?.path) await filter.apply(video.path).then((r) => (res = r.value));
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
	<form on:submit={submit}>
		<ion-list>
			<ion-item>
				{#if video}
					<ion-text>{video.name}</ion-text>
				{:else}
					<div />
				{/if}
				<ion-button on:click={pickVideos} slot="end">pick a video</ion-button>
			</ion-item>
			<ion-item>
				<ion-select
					label="Choose a filter"
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
			<ion-item>
				<ion-button type="submit" slot="end">apply</ion-button>
			</ion-item>
			<ion-item>
				{filter?.getCommand()}
				<ion-item>
					{#key res}
						<Logs logString={res} />
					{/key}
				</ion-item>
			</ion-item></ion-list
		>
	</form>
</ion-content>
