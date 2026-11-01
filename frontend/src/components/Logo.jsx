import React from 'react'

export default function Logo({ className = "w-7 h-7", textClassName = "text-xl font-bold tracking-tight text-navy-900", showText = true }) {
  return (
    <div className="flex items-center gap-2.5 select-none">
      <div className={`relative flex items-center justify-center bg-navy-900 rounded-lg p-1.5 shadow-sm text-white ${className}`}>
        {/* Geometric Lens + AI Sparkle icon */}
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
          {/* Outer diamond lens shape */}
          <path 
            d="M12 2L14.8 9.2L22 12L14.8 14.8L12 22L9.2 14.8L2 12L9.2 9.2L12 2Z" 
            fill="currentColor" 
            fillOpacity="0.95"
          />
          {/* Inner focal core */}
          <circle cx="12" cy="12" r="3.2" fill="#3b82f6" />
          <circle cx="12" cy="12" r="1.4" fill="#ffffff" />
        </svg>
      </div>
      {showText && (
        <span className={textClassName}>
          PROJECT<span className="font-extrabold tracking-normal text-navy-900">LENSE</span>
        </span>
      )}
    </div>
  )
}
