<script lang="ts">
	import type { Filter } from '$lib/ffmpeg/FFmpeg';
	import zodKeys from '$lib/utils/zodKeys';
	import { writable } from 'svelte/store';
	import { ZodEnum } from 'zod';

	type StringField = {
		type: 'string';
		name: string;
		defaultValue: string;
	};

	type NumberField = {
		type: 'number';
		name: string;
		defaultValue: number;
	};

	type BooleanField = {
		type: 'boolean';
		name: string;
		defaultValue: boolean;
	};

	type ListField = {
		type: 'enum';
		name: string;
		defaultValue: number;
		list: any;
	};

	type Field = StringField | NumberField | BooleanField | ListField;

	export let filter: Filter<any>;
	export let submit: () => void;

	$: getFields = () => {
		const keys = zodKeys(filter.schema!);
		const fields: Field[] = [];
		keys.forEach((k) => {
			const keyType = filter.schema!.shape[k];
			if (keyType instanceof ZodEnum) {
				fields.push({
					name: k,
					type: 'enum',
					defaultValue: filter.parameters[k],
					list: (keyType as ZodEnum<[string, ...string[]]>).Values
				});
			} else if (typeof filter.parameters[k] === 'string') {
				fields.push({ name: k, type: 'string', defaultValue: filter.parameters[k] });
			} else if (typeof filter.parameters[k] === 'number') {
				fields.push({ name: k, type: 'number', defaultValue: filter.parameters[k] });
			} else if (typeof filter.parameters[k] === 'boolean') {
				fields.push({ name: k, type: 'boolean', defaultValue: filter.parameters[k] });
			}
		});
		return fields;
	};
	const store = writable(filter.parameters);

	const onSubmit = () => {
		filter.setParameters($store);
		submit();
	};
</script>

{#if filter?.parameters}
	<form on:submit|preventDefault={onSubmit}>
		{#each getFields() as field}
			{@const { name } = field}
			<ion-item>
				{#if field.type === 'string'}
					<ion-input
						type="text"
						{name}
						label={name}
						value={field.defaultValue}
						on:ionInput={(e) => {
							$store[name] = e.target.value;
						}}
					/>
				{:else if field.type === 'number'}
					<ion-input
						type="number"
						{name}
						label={name}
						value={$store[name]}
						on:ionInput={(e) => {
							$store[name] = Number(e.target.value);
						}}
					/>
				{:else if field.type === 'boolean'}
					<ion-checkbox {name} checked={field.defaultValue} />
				{:else if field.type === 'enum'}
					<ion-select label={field.name} placeholder={field.defaultValue.toString()}>
						{#each field.list as option}
							<ion-select-option value={option}>{option}</ion-select-option>
						{/each}
					</ion-select>
				{/if}
			</ion-item>
		{/each}
		<ion-item>
			<ion-button type="submit" slot="end">apply</ion-button>
		</ion-item>
	</form>
{/if}
