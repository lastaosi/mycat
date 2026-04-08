//
//  MainTabView.swift
//  iosApp
//
//  Created by LeeJungHoon on 4/8/26.
//
import SwiftUI
struct MainTabView: View{
    var body: some View{
        TabView{
            HomeView()
                .tabItem{
                    Label("홈", systemImage:"house.fill")
                }
            
            CardView()
                .tabItem{
                    Label("카드", systemImage:"pill.fill")
                }
            DiaryView()
                .tabItem{
                    Label("다이어리", systemImage:"note.text")
                }
            MoreView()
                .tabItem{
                    Label("더보기", systemImage:"ellipsis")
                }
        }
        .tint(MyCatColors.primary)
    }
    
    
}


struct CardView: View{
    var body: some View{
        PlaceholderView(title:"케어"){}
    }
}

struct DiaryView: View{
    var body: some View{
        PlaceholderView(title:"다이어리"){}
    }
}
struct MoreView:View{
    var body: some View{
        PlaceholderView(title:"더보기"){}
    }
}
