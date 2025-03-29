import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements AfterViewInit, OnDestroy {
  private resizeObserver: ResizeObserver | null = null;

  ngAfterViewInit(): void {
    this.adjustLayout();
    this.setupResizeObserver();
  }

  ngOnDestroy(): void {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  private adjustLayout(): void {
    const mainContainer = document.querySelector('.main-container') as HTMLElement;
    const footer = document.querySelector('.footer') as HTMLElement;
    const contentContainer = document.querySelector('.content-container') as HTMLElement;

    if (!mainContainer || !footer || !contentContainer) return;

    const footerHeight = footer.offsetHeight;
    const windowHeight = window.innerHeight;
    const contentHeight = contentContainer.offsetHeight;

    // Ensure the main container takes up at least the viewport height minus footer
    mainContainer.style.minHeight = `${windowHeight}px`;

    // If content is shorter than viewport minus footer, adjust padding
    if (contentHeight + footerHeight < windowHeight) {
      contentContainer.style.minHeight = `${windowHeight - footerHeight}px`;
    } else {
      contentContainer.style.minHeight = 'auto'; // Reset if content is long enough
    }
  }

  private setupResizeObserver(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.adjustLayout();
    });
    this.resizeObserver.observe(document.body);

    // Also listen to window resize for immediate updates
    window.addEventListener('resize', this.adjustLayout.bind(this));
  }
}
