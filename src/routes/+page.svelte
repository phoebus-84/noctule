<!-- <script lang="ts">
	import Echo from '$lib/nativeHooks/EchoPlugin';

	let res: any;
	const p = () => {
		Echo.echo({
			value:
				'-i /storage/emulated/0/Download/testsrc.mpg -vf "frei0r=colordistance:0.2/0.3/0.4" /storage/emulated/0/DCIM/tesst3.avi'
		}).then((r) => {
			res = r;
		});
	};
</script>

<ion-header>
	<ion-toolbar>
		<ion-buttons slot="start">
			<ion-back-button />
		</ion-buttons>
		<ion-title>ffmpeg</ion-title>
	</ion-toolbar>
</ion-header>
<ion-content>
	<ion-button on:click={p}>popo</ion-button>
	{#if res}
		<pre>
				{JSON.stringify(res.value, null, 2)}
			</pre>
		<ion-input value={JSON.stringify(res, null, 2)}></ion-input>
	{/if}
</ion-content> -->

<script lang="ts">
	import { Filter, filters } from '$lib/ffmpeg/FFmpeg';
	import Logs from '$lib/logs/Logs.svelte';
	import { FilePicker } from '@capawesome/capacitor-file-picker';

	let video: { name: string | undefined; path: string | undefined; video: any } | undefined;
	let filter: Filter<unknown>;
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
	function replacePatternWithString(inputString: string) {
		const pattern = /%\d+[A-Z]/g;
		return inputString.replace(pattern, '/').split('content:/')[1];
	}
</script>

<ion-header translucent={true}>
	<ion-toolbar>
		<ion-buttons slot="start">
			<ion-menu-button />
		</ion-buttons>
		<ion-title>Frei0r</ion-title>
	</ion-toolbar>
</ion-header>

<ion-content fullscreen class="ion-padding">
	<ion-card>
		<ion-card-content>
			Frei0r is a minimalistic plugin API for video effects. The main emphasis is on simplicity for an API that will
			round up the most common video effects into simple filters, sources and mixers that can be controlled by
			parameters. It’s our hope that this way these simple effects can be shared between many applications, avoiding
			their reimplementation by different projects.
		</ion-card-content>
	</ion-card>
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
				{#key res}
					<Logs logString={res} />
				{/key}
			</ion-item>
		</ion-list>
	</form>
</ion-content>
