import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">SM</span>
        <span class="brand-name">Shipping<span class="mono muted">/wms</span></span>
      </div>
      <nav>
        <a routerLink="/orders" routerLinkActive="active">Aufträge</a>
        <a routerLink="/shipments" routerLinkActive="active">Sendungen</a>
      </nav>
    </header>
    <main class="content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [
    `
      .topbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px 32px;
        border-bottom: 1px solid var(--border);
        background: var(--surface);
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .brand-mark {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 30px;
        height: 30px;
        border-radius: 6px;
        background: var(--accent);
        color: var(--accent-contrast);
        font-family: var(--font-mono);
        font-weight: 600;
        font-size: 13px;
      }
      .brand-name {
        font-weight: 600;
        font-size: 16px;
        letter-spacing: -0.01em;
      }
      .muted {
        color: var(--text-muted);
        font-weight: 400;
      }
      nav {
        display: flex;
        gap: 24px;
      }
      nav a {
        text-decoration: none;
        color: var(--text-muted);
        font-size: 14px;
        font-weight: 500;
        padding: 6px 2px;
        border-bottom: 2px solid transparent;
      }
      nav a.active {
        color: var(--text);
        border-bottom-color: var(--accent);
      }
      .content {
        max-width: 960px;
        margin: 0 auto;
        padding: 32px;
      }
    `,
  ],
})
export class AppComponent {}
