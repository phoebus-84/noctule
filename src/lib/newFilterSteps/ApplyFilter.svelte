<script lang="ts">
	import type { Writable } from 'svelte/store';
	import type { StepperContext } from '../../routes/new/+page.svelte';
	import { createStepController } from '@efstajas/svelte-stepper';
	import { filters } from '$lib/ffmpeg/FFmpeg';
	import ParametersFields from '$lib/forms/ParametersFields.svelte';

	const stepController = createStepController();

	export let context: Writable<StepperContext>;

	let res;
	let loading;

	const submit = async () => {
		loading = true;
		res = await $context.filter?.apply($context.video!.path!).then((r) => (res = r.value));
		loading = false;
		stepController.nextStep();
	};
</script>

<ion-content fullscreen class="ion-padding">
	<ion-list>
		<ion-item>
			<ion-text>{$context.video?.name}</ion-text>
			<ion-button on:click={() => goto('/new')} slot="end">pick a video</ion-button>
		</ion-item>
		<ion-item>
			<ion-select
				label="Choose a filter"
				placeholder="..."
				on:ionChange={(e) => {
					$context.filter = e.detail.value;
				}}
			>
				{#each filters as filter}
					<ion-select-option value={filter}>{filter.name}</ion-select-option>
				{/each}
			</ion-select>
		</ion-item>
		{#if $context.filter?.parameters}
			<ParametersFields filter={$context.filter} {submit} />
		{:else}
			<ion-item>
				<ion-button slot="end" on:click={submit}>apply</ion-button>
			</ion-item>
		{/if}
	</ion-list>
</ion-content>
