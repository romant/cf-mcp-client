import { Injectable } from '@angular/core';
import { MatSidenav } from '@angular/material/sidenav';

@Injectable({
  providedIn: 'root'
})
export class SidenavService {
  private sidenavs: { [key: string]: MatSidenav } = {};

  registerSidenav(id: string, sidenav: MatSidenav): void {
    this.sidenavs[id] = sidenav;
  }

  open(id: string): void {
    // Close all other sidenavs
    Object.entries(this.sidenavs).forEach(([sidenavId, sidenav]) => {
      if (sidenavId !== id && sidenav.opened) {
        sidenav.close();
      }
    });

    // Open the requested sidenav
    const sidenav = this.sidenavs[id];
    if (sidenav && !sidenav.opened) {
      sidenav.open();
    }
  }

  close(id: string): void {
    const sidenav = this.sidenavs[id];
    if (sidenav && sidenav.opened) {
      sidenav.close();
    }
  }

  toggle(id: string): void {
    const sidenav = this.sidenavs[id];
    if (!sidenav) return;

    if (sidenav.opened) {
      sidenav.close();
    } else {
      this.open(id);
    }
  }
}
