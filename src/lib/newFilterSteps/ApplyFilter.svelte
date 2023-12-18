<script lang="ts">
	import type { Writable } from 'svelte/store';
	import type { StepperContext } from '../../routes/new/+page.svelte';
	import { createStepController } from '@efstajas/svelte-stepper';
	import { filters } from '$lib/ffmpeg/FFmpeg';
	import ParametersFields from '$lib/forms/ParametersFields.svelte';

	const stepController = createStepController();

	export let context: Writable<StepperContext>;

	let loading = false;

	const submit = async () => {
		loading = true;
		$context.results = await $context.filter?.apply($context.video!.path!);
		loading = false;
		stepController.nextStep();
	};
</script>

{#if !loading}
	<ion-list>
		<ion-item>
			<ion-label class="font-bold text-blue-400">Video:</ion-label>
		</ion-item>
		<ion-item>
			<ion-text>{$context.video?.name}</ion-text>
		</ion-item>
		<ion-item>
			<ion-label class="font-bold text-blue-400">Choose your filter:</ion-label>
		</ion-item>
		<ion-item>
			<ion-select
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
			<ion-item>
				<ion-label class="font-bold text-blue-400">Set up the parameters:</ion-label>
			</ion-item>
			<ParametersFields filter={$context.filter} {submit} />
		{:else}
			<ion-item>
				<ion-button on:click={submit}>apply filter</ion-button>
			</ion-item>
		{/if}
	</ion-list>
{:else}
	<ion-spinner />
{/if}
