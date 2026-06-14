import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-politica-privacidad',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './politica-privacidad.html',
  styleUrls: ['./politica-privacidad.scss']
})
export class PoliticaPrivacidad {}
