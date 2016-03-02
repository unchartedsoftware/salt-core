declare module 'aurelia-pal-browser' {
  import 'core-js';
  import { initializePAL }  from 'aurelia-pal';
  
  /**
  * Initializes the PAL with the Browser-targeted implementation.
  */
  export function initialize(): void;
}