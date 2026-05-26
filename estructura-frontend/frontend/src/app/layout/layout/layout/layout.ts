import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../../sidebar/sidebar/sidebar';
import { Header } from '../../header/header/header';
import { SidebarStateService } from '../../../core/services/sidebar-state.service';


@Component({
  selector: 'app-layout',
  standalone:true,
  imports: [RouterOutlet,CommonModule, RouterModule,Sidebar,Header],
  templateUrl: './layout.html',
  styleUrls: ['./layout.scss'],
})
export class Layout {
  constructor(public sidebarState: SidebarStateService){}
}
