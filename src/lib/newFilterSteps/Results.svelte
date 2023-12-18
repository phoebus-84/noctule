<script lang="ts">
  import type { Writable } from 'svelte/store';
  import type { StepperContext } from '../../routes/new/+page.svelte';
  import { createStepController } from '@efstajas/svelte-stepper';

  const stepController = createStepController();

  export let context: Writable<StepperContext>;
</script>

<h1>My first step</h1>

<p>
  Foo is {$context.video} and bar is {$context.filter}.
</p>

<!-- <button on:click={context.update((v) => ({ ...v, bar: v.bar + 1}))}>Increment bar</button> -->

<button on:click={stepController.nextStep}>Next</button>