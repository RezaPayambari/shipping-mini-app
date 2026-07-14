import { Component, Input } from '@angular/core';
import { ShipmentStatus } from '../models/models';

@Component({
  selector: 'app-status-pipeline',
  standalone: true,
  imports: [],
  template: `
    <div class="pipeline" role="img" [attr.aria-label]="'Status: ' + status">
      @for (step of steps; track step; let last = $last) {
        <div class="node" [class.done]="isDone(step)" [class.current]="step === status">
          <span class="dot"></span>
          <span class="label">{{ step }}</span>
        </div>
        @if (!last) {
          <div class="connector" [class.done]="isDone(step)"></div>
        }
      }
    </div>
  `,
  styles: [
    `
      .pipeline {
        display: flex;
        align-items: center;
        gap: 0;
      }
      .node {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        min-width: 84px;
      }
      .dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: var(--border);
        border: 2px solid var(--border);
      }
      .node.done .dot {
        background: var(--status-shipped);
        border-color: var(--status-shipped);
      }
      .node.current .dot {
        background: var(--accent);
        border-color: var(--accent);
        box-shadow: 0 0 0 4px rgba(255, 122, 26, 0.2);
      }
      .label {
        font-family: var(--font-mono);
        font-size: 11px;
        letter-spacing: 0.06em;
        color: var(--text-muted);
      }
      .node.current .label {
        color: var(--text);
        font-weight: 600;
      }
      .connector {
        height: 2px;
        flex: 1;
        background: var(--border);
        margin-bottom: 20px;
        min-width: 24px;
      }
      .connector.done {
        background: var(--status-shipped);
      }
    `,
  ],
})
export class StatusPipelineComponent {
  @Input({ required: true }) status!: ShipmentStatus;

  readonly steps: ShipmentStatus[] = ['CREATED', 'PACKED', 'SHIPPED'];

  isDone(step: ShipmentStatus): boolean {
    return this.steps.indexOf(step) < this.steps.indexOf(this.status);
  }
}
